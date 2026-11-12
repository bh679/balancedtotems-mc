package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.net.MorphSyncPayload;
import games.brennan.balancedtotems.net.CurseCountPayload;
import games.brennan.balancedtotems.net.TotemPopPayload;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Client side of every payload. Only ever reached from a payload handler on the client. */
public final class ClientPayloads {

    private static final int POP_PARTICLE_TICKS = 30;

    /** Your view before a morph put you in third person, restored when the form ends; null when we did not switch. */
    @Nullable
    private static CameraType cameraBeforeMorph;

    private ClientPayloads() {}

    /** Vanilla's entity event 35, but showing the totem that actually popped. */
    public static void onTotemPop(TotemPopPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Entity entity = level.getEntity(payload.entityId());
        if (entity == null) return;
        mc.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, POP_PARTICLE_TICKS);
        level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE,
            entity.getSoundSource(), 1.0F, 1.0F, false);
        if (entity == mc.player) mc.player.displayItemActivation(payload.totem());
    }

    public static void onMorphSync(MorphSyncPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (!(level.getEntity(payload.entityId()) instanceof Player player)) return;
        player.setData(BtRegistries.FORM, payload.form());
        player.refreshDimensions();
        MorphRenderer.forget(player);
        if (player == Minecraft.getInstance().player) followMorphWithCamera(player, payload.form());
    }

    /** Morphing turns your camera to third person so you can see what you became; it goes back after. */
    private static void followMorphWithCamera(Player player, String form) {
        Minecraft mc = Minecraft.getInstance();
        if (!form.isEmpty() && player.hasEffect(BtRegistries.MORPHED)) {
            if (cameraBeforeMorph == null) cameraBeforeMorph = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (form.isEmpty() && cameraBeforeMorph != null) {
            mc.options.setCameraType(cameraBeforeMorph);
            cameraBeforeMorph = null;
        }
    }

    public static void onCurseCount(CurseCountPayload payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setData(payload.curse().equals("LONELINESS") ? BtRegistries.LONELY_FRIENDS : BtRegistries.RAMPAGE_KILLS, payload.count());
    }
}
