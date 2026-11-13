package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.net.CurseTimerPayload;
import games.brennan.dungeontotems.net.TotemNetwork;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Rage and Lonely are timers. Rage: hit something before it runs out. Lonely: give a Player Mob or a
 * villager a gift ({@link LonelyGifts}). Miss it and you take true damage at Poison's rate until you do.
 * The higher the level, the shorter the timer. The time left goes to your client for the countdown
 * border and the hover text.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class CurseTimers {

    /** Seconds you get at level I, II and III (and above). */
    private static final int[] SECONDS_BY_LEVEL = {60, 40, 30};
    /** Ticks between hits once the timer has run out: Poison's rate. */
    public static final int DAMAGE_INTERVAL_TICKS = 25;
    public static final float DAMAGE = 1.0F;
    /** How often the timer goes to your client, often enough for the countdown border to drain smoothly. */
    private static final int SYNC_TICKS = 5;

    private static final List<Curse> TIMED = List.of(Curse.RAGE, Curse.LONELINESS);

    private CurseTimers() {}

    /** Seconds the timer gives at {@code amplifier}. */
    public static int seconds(int amplifier) {
        return SECONDS_BY_LEVEL[Math.min(amplifier, SECONDS_BY_LEVEL.length - 1)];
    }

    /** A fresh Rage or Lonely curse: its timer starts full. */
    public static void restart(ServerPlayer player, Curse curse) {
        if (TIMED.contains(curse)) player.setData(last(curse), player.level().getGameTime());
    }

    /** You gave a gift: Lonely's timer starts again. */
    public static void gift(ServerPlayer player) {
        player.setData(TotemRegistries.LAST_GIFT, player.level().getGameTime());
    }

    /** You hurt something other than yourself: Rage's timer starts again. */
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            attacker.setData(TotemRegistries.LAST_HIT, attacker.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        for (Curse curse : TIMED) tick(player, curse);
    }

    private static void tick(ServerPlayer player, Curse curse) {
        MobEffectInstance instance = player.getEffect(curse.effect());
        if (instance == null) return;
        long overdue = player.level().getGameTime() - player.getData(last(curse)) - seconds(instance.getAmplifier()) * 20L;
        if (overdue >= 0 && overdue % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(damage(player, curse), DAMAGE);
        }
        if (player.tickCount % SYNC_TICKS == 0) {
            int total = seconds(instance.getAmplifier()) * 20;
            TotemNetwork.sendTo(player, new CurseTimerPayload(curse.name(), (int) Math.max(0, -overdue), total));
        }
    }

    private static DamageSource damage(ServerPlayer player, Curse curse) {
        ResourceKey<DamageType> key = curse == Curse.RAGE ? TotemRegistries.RAGE_FAILED : TotemRegistries.LONELINESS;
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return new DamageSource(type);
    }

    private static Supplier<AttachmentType<Long>> last(Curse curse) {
        return curse == Curse.RAGE ? TotemRegistries.LAST_HIT : TotemRegistries.LAST_GIFT;
    }
}
