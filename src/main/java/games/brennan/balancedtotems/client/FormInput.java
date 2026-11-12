package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormAttacks;
import games.brennan.balancedtotems.morph.FormUse;
import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.net.FormAttackPayload;
import games.brennan.balancedtotems.net.FormUsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "Use" with an empty hand as a form with an extra move asks the server to do it (see {@link FormUse});
 * attacking the air as a form with a ranged attack (llama, snow golem) asks it to shoot.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class FormInput {

    private FormInput() {}

    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null || !FormUse.USABLE.contains(form) || !player.getMainHandItem().isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(FormUsePayload.INSTANCE);
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !event.isAttack()) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        boolean air = minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS;
        if (form == null || !FormAttacks.RANGED.contains(form) || !air) return; // mobs and blocks go the usual way
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketDistributor.sendToServer(FormAttackPayload.INSTANCE);
    }
}
