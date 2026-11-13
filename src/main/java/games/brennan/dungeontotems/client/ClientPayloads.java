package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.net.MorphSyncPayload;
import games.brennan.dungeontotems.net.CurseTimerPayload;
import games.brennan.dungeontotems.net.MorphActionPayload;
import games.brennan.dungeontotems.net.TotemPopPayload;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client side of every payload. Only ever reached from a payload handler on the client. */
public final class ClientPayloads {

    private static final int POP_PARTICLE_TICKS = 30;

    /** Your view before a morph put you in third person, restored when the form ends; null when we did not switch. */
    @Nullable
    private static CameraType cameraBeforeMorph;
    /** The view a morph switches you to. */
    private static final CameraType MORPH_CAMERA = CameraType.THIRD_PERSON_BACK;

    /** The last timer the server sent for each timed curse, by Curse name. */
    private static final Map<String, CurseTimerPayload> TIMERS = new ConcurrentHashMap<>();

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
        if (entity == mc.player) mc.gameRenderer.displayItemActivation(payload.totem());
    }

    public static void onMorphSync(MorphSyncPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (!(level.getEntity(payload.entityId()) instanceof Player player)) return;
        player.setData(TotemRegistries.FORM, payload.form());
        player.refreshDimensions();
        MorphRenderer.forget(player);
        if (player == Minecraft.getInstance().player) followMorphWithCamera(player, payload.form());
    }

    /**
     * Morphing turns your camera to third person so you can see what you became. When it wears off the
     * camera goes back to what it was, unless you changed it yourself in the meantime.
     */
    private static void followMorphWithCamera(Player player, String form) {
        Minecraft mc = Minecraft.getInstance();
        if (!form.isEmpty() && player.hasEffect(TotemRegistries.MORPHED)) {
            if (cameraBeforeMorph == null) cameraBeforeMorph = mc.options.getCameraType();
            mc.options.setCameraType(MORPH_CAMERA);
        } else if (form.isEmpty() && cameraBeforeMorph != null) {
            if (mc.options.getCameraType() == MORPH_CAMERA) mc.options.setCameraType(cameraBeforeMorph);
            cameraBeforeMorph = null;
        }
    }

    public static void onMorphAction(MorphActionPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(payload.entityId()) instanceof Player player) {
            MorphRenderer.perform(player, payload.action());
        }
    }

    public static void onCurseTimer(CurseTimerPayload payload) {
        TIMERS.put(payload.curse(), payload);
    }

    /** Seconds left on your Rage or Lonely timer, as the server last said; 0 when unknown. */
    public static int secondsLeft(Curse curse) {
        CurseTimerPayload timer = TIMERS.get(curse.name());
        return timer == null ? 0 : (timer.ticksLeft() + 19) / 20;
    }

    /** How much of the Rage or Lonely timer is left, 1 (full) to 0 (time's up); 1 when unknown. */
    public static float fractionLeft(Curse curse) {
        CurseTimerPayload timer = TIMERS.get(curse.name());
        if (timer == null || timer.totalTicks() <= 0) return 1.0F;
        return Math.min(1.0F, timer.ticksLeft() / (float) timer.totalTicks());
    }
}
