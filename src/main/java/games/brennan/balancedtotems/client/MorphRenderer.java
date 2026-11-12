package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormAction;
import games.brennan.balancedtotems.morph.Morphs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a morphed player as their form: the player model is cancelled and a client-only dummy of
 * the mob is drawn in its place, posed from the player every frame and animated every tick.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class MorphRenderer {

    /** One dummy per morphed player, rebuilt when their form changes. */
    private static final Map<UUID, LivingEntity> DUMMIES = new ConcurrentHashMap<>();
    /** How far a form without an eat pose lowers its head to eat. */
    private static final float EATING_PITCH = 60.0F;

    private MorphRenderer() {}

    /** The stand-in drawn for a morphed player, if there is one yet. */
    static LivingEntity standIn(Player player) {
        return DUMMIES.get(player.getUUID());
    }

    static void forget(Player player) {
        DUMMIES.remove(player.getUUID());
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        LivingEntity dummy = dummyFor(player);
        if (dummy == null) return;

        event.setCanceled(true);
        poseLike(dummy, player);
        EntityRenderer<Entity> renderer =
            (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
        renderer.render(dummy, player.getYRot(), event.getPartialTick(), event.getPoseStack(),
            event.getMultiBufferSource(), event.getPackedLight());
    }

    /** A morphed player has no hands to show in first person. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && Morphs.formOf(player).isPresent()) event.setCanceled(true);
    }

    /** Animations advance once a tick (see {@link FormAnimator}). */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            DUMMIES.clear();
            return;
        }
        for (Player player : level.players()) {
            LivingEntity dummy = DUMMIES.get(player.getUUID());
            if (dummy != null) FormAnimator.tick(dummy, player);
        }
        DUMMIES.keySet().removeIf(id -> {
            boolean gone = level.getPlayerByUUID(id) == null;
            if (gone) FormAnimator.forget(id);
            return gone;
        });
    }

    /** Shows a form's action (a ram, a swing, a graze) on a morphed player's stand-in. */
    static void perform(Player player, int action) {
        LivingEntity dummy = dummyFor(player);
        if (dummy != null && action >= 0 && action < FormAction.values().length) {
            FormAnimator.perform(dummy, player.getUUID(), FormAction.values()[action]);
        }
    }

    private static LivingEntity dummyFor(Player player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null) {
            DUMMIES.remove(player.getUUID());
            return null;
        }
        LivingEntity existing = DUMMIES.get(player.getUUID());
        if (existing != null && existing.getType() == form) return existing;
        if (!(form.create(player.level()) instanceof LivingEntity created)) return null;
        if (created instanceof Bat bat) bat.setResting(false); // a new bat hangs upside down
        created.setSilent(true); // its own tick runs for animations; the player makes the sounds
        created.setPos(player.getX(), player.getY(), player.getZ());
        created.setOldPosAndRot();
        DUMMIES.put(player.getUUID(), created);
        return created;
    }

    /** Copies look, body turn, motion state and held/worn items from the player. */
    private static void poseLike(LivingEntity dummy, Player player) {
        dummy.setYRot(player.getYRot());
        dummy.yRotO = player.yRotO;
        float pitch = FormAnimator.headDown(player.getUUID()) ? EATING_PITCH : player.getXRot();
        dummy.setXRot(pitch);
        dummy.xRotO = FormAnimator.headDown(player.getUUID()) ? EATING_PITCH : player.xRotO;
        Float curled = FormAnimator.curledYaw(player.getUUID()); // a curled armadillo doesn't turn with you
        dummy.yBodyRot = curled != null ? curled : player.yBodyRot;
        dummy.yBodyRotO = curled != null ? curled : player.yBodyRotO;
        dummy.yHeadRot = curled != null ? curled : player.yHeadRot;
        dummy.yHeadRotO = curled != null ? curled : player.yHeadRotO;
        dummy.setOnGround(player.onGround());
        dummy.setShiftKeyDown(player.isShiftKeyDown());
        dummy.setInvisible(player.isInvisible());
        dummy.hurtTime = player.hurtTime;
        dummy.deathTime = player.deathTime;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ANIMAL_ARMOR) continue;
            dummy.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }
}
