package games.brennan.dungeontotems.morph;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Empty-hand "use" moves for water forms: play dead, puff up, ink, leap. */
public final class WaterMoves {

    private static final int PLAY_DEAD_TICKS = 200;
    private static final int PUFF_TICKS = 100;
    /** {@code Pufferfish#touch} at full puff: 60 ticks per puff level. */
    private static final int PUFF_POISON_TICKS = 120;
    private static final int INK_COOLDOWN = 100;
    private static final int INK_BLIND_TICKS = 60;
    private static final double INK_RADIUS = 5.0;
    private static final double LOSE_YOU_RADIUS = 16.0;
    private static final int LEAP_COOLDOWN = 20;
    private static final double LEAP_SPEED = 1.0;
    private static final double LEAP_LIFT = 0.7;
    private static final ResourceLocation PLAY_DEAD_SPEED_ID = DungeonTotems.id("morph.play_dead_speed");

    private static final Set<UUID> PLAYING_DEAD = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> PUFFED = ConcurrentHashMap.newKeySet();

    private WaterMoves() {}

    /** {@code PlayDead}: lie still for 10 s, healing, while every mob loses track of you. */
    static void playDead(ServerPlayer player) {
        if (!PLAYING_DEAD.add(player.getUUID())) return;
        FormUse.hold(player, Attributes.MOVEMENT_SPEED, PLAY_DEAD_SPEED_ID, true);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, PLAY_DEAD_TICKS, 0));
        loseMobs(player);
        FormTimers.show(player, FormAction.PLAY_DEAD);
        FormTimers.later(player, PLAY_DEAD_TICKS, WaterMoves::wake);
    }

    private static void wake(ServerPlayer player) {
        if (!PLAYING_DEAD.remove(player.getUUID())) return;
        FormUse.hold(player, Attributes.MOVEMENT_SPEED, PLAY_DEAD_SPEED_ID, false);
        FormTimers.show(player, FormAction.WAKE);
    }

    public static boolean isPlayingDead(Player player) {
        return PLAYING_DEAD.contains(player.getUUID());
    }

    /** Puff up for 5 s: whatever touches you is poisoned. */
    static void puff(ServerPlayer player) {
        if (!PUFFED.add(player.getUUID())) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormTimers.show(player, FormAction.PUFF);
        FormTimers.later(player, PUFF_TICKS, WaterMoves::deflate);
    }

    private static void deflate(ServerPlayer player) {
        if (!PUFFED.remove(player.getUUID())) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormTimers.show(player, FormAction.DEFLATE);
    }

    /** {@code Pufferfish#touch}, every tick while puffed. */
    static void tickPuffed(ServerPlayer player) {
        if (!PUFFED.contains(player.getUUID())) return;
        for (LivingEntity touching : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.3),
            entity -> entity != player && !entity.hasEffect(MobEffects.POISON))) {
            touching.addEffect(new MobEffectInstance(MobEffects.POISON, PUFF_POISON_TICKS, 0), player);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** {@code Squid#spawnInk}: a cloud of ink; nearby players can't see, nearby mobs lose you. */
    static void ink(ServerPlayer player) {
        if (!FormTimers.ready(player, "ink", INK_COOLDOWN)) return;
        boolean glow = Morphs.formOf(player).orElse(null) == EntityType.GLOW_SQUID;
        Vec3 at = player.position().add(0.0, player.getBbHeight() / 2.0, 0.0);
        player.serverLevel().sendParticles(glow ? ParticleTypes.GLOW_SQUID_INK : ParticleTypes.SQUID_INK,
            at.x, at.y, at.z, 30, 0.4, 0.4, 0.4, 0.1);
        player.level().playSound(null, player.blockPosition(),
            glow ? SoundEvents.GLOW_SQUID_SQUIRT : SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (Player other : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(INK_RADIUS), p -> p != player)) {
            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, INK_BLIND_TICKS, 0), player);
        }
        loseMobs(player);
    }

    /** A dolphin's leap: forward and up, from the water. */
    static void leap(ServerPlayer player) {
        if (!player.isInWater() || !FormTimers.ready(player, "leap", LEAP_COOLDOWN)) return;
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        player.setDeltaMovement(look.scale(LEAP_SPEED).add(0.0, LEAP_LIFT, 0.0));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.blockPosition(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void loseMobs(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(LOSE_YOU_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    /** From {@link Morphs#setForm}: a new form stops playing dead and deflates. */
    static void onFormChanged(ServerPlayer player) {
        if (PLAYING_DEAD.remove(player.getUUID())) FormUse.hold(player, Attributes.MOVEMENT_SPEED, PLAY_DEAD_SPEED_ID, false);
        PUFFED.remove(player.getUUID());
    }

    /** Test seam. */
    public static boolean isPuffed(Player player) {
        return PUFFED.contains(player.getUUID());
    }
}
