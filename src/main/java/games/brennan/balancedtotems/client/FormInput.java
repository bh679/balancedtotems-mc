package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormUse;
import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.net.FormAttackPayload;
import games.brennan.balancedtotems.net.FormJumpPayload;
import games.brennan.balancedtotems.net.FormUsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A morphed player's keys, sent to the server where the form does something of its own:
 * <ul>
 *   <li>"Use" with an empty hand: the form's move, or its sound ({@link FormUse}). Doors, chests,
 *       buttons, levers, beds and the like still work, and you can still trade with villagers.</li>
 *   <li>"Attack" on the air: a shot (llama, snow golem), a goat's ram, or the form's sound. Attacks on mobs
 *       and blocks go the usual way.</li>
 *   <li>"Jump" as a camel: its dash. Moving while curled up as an armadillo: it unrolls.</li>
 * </ul>
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class FormInput {

    /** Ticks between unroll requests while you keep pushing to move. */
    private static final int UNROLL_RETRY_TICKS = 10;

    private static boolean jumpWasDown;
    private static int unrollWait;

    private FormInput() {}

    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (Morphs.formOf(player).isEmpty() || !player.getMainHandItem().isEmpty()) return;
        HitResult hit = minecraft.hitResult;
        if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof Merchant) return; // you can still trade
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && FormUse.isUsable(player.level(), block.getBlockPos())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(FormUsePayload.INSTANCE);
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !event.isAttack() || Morphs.formOf(player).isEmpty()) return;
        boolean air = minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS;
        if (!air) return; // mobs and blocks go the usual way
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(FormAttackPayload.INSTANCE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        EntityType<?> form = player == null ? null : Morphs.formOf(player).orElse(null);
        boolean jumpDown = minecraft.options.keyJump.isDown();
        if (form != null && FormUse.OWN_JUMP.contains(form) && jumpDown && !jumpWasDown && minecraft.screen == null) {
            PacketDistributor.sendToServer(FormJumpPayload.INSTANCE);
        }
        jumpWasDown = jumpDown;

        if (unrollWait > 0) unrollWait--;
        if (form == EntityType.ARMADILLO && unrollWait == 0 && FormAnimator.isCurled(player.getUUID()) && player.input.getMoveVector().lengthSquared() > 0.0F) {
            PacketDistributor.sendToServer(FormUsePayload.INSTANCE); // "use" toggles the roll: out it comes
            unrollWait = UNROLL_RETRY_TICKS;
        }
    }
}
