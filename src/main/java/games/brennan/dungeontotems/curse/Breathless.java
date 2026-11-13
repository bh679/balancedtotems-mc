package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Breathless: out of water your air drains as if you were under it, and runs out into drowning on land,
 * 2 damage each time the bar empties, like vanilla. Under water vanilla already drains it. Water
 * Breathing and Conduit Power keep you breathing, as they would under water.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class Breathless {

    /** Vanilla's drowning: the bar runs this far below empty before each hit. */
    private static final int DROWN_AT = -20;
    private static final float DROWN_DAMAGE = 2.0F;

    /** Air at the start of this tick, before vanilla refilled it. */
    private static final Map<UUID, Integer> AIR_BEFORE = new ConcurrentHashMap<>();

    private Breathless() {}

    @SubscribeEvent
    public static void onTickStart(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) AIR_BEFORE.put(player.getUUID(), player.getAirSupply());
    }

    @SubscribeEvent
    public static void onTickEnd(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer before = AIR_BEFORE.remove(player.getUUID());
        if (before == null || !suffocates(player)) return;
        int air = before - 1;
        if (air <= DROWN_AT) {
            air = 0;
            DamageSource source = new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TotemRegistries.SUFFOCATION));
            player.hurt(source, DROWN_DAMAGE);
        }
        player.setAirSupply(air);
    }

    /** Breathless, out of water, and nothing to breathe with. */
    static boolean suffocates(ServerPlayer player) {
        if (!player.isAlive() || !player.hasEffect(TotemRegistries.BREATHLESS)) return false;
        if (player.isEyeInFluid(FluidTags.WATER) || player.getAbilities().invulnerable) return false;
        return !player.hasEffect(MobEffects.WATER_BREATHING) && !player.hasEffect(MobEffects.CONDUIT_POWER);
    }
}
