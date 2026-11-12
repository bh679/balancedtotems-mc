package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Curses follow you through death: each comes back on respawn at the same level with a full timer.
 * A deadline curse that killed you by running out is already gone, so it stays gone. Read on the
 * clone (the dead body still holds its effects), applied once the new body is in the world.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class CurseMemory {

    private static final Map<UUID, Map<Curse, Integer>> KEPT = new ConcurrentHashMap<>();

    private CurseMemory() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Map<Curse, Integer> curses = new EnumMap<>(Curse.class);
        for (MobEffectInstance instance : event.getOriginal().getActiveEffects()) {
            Curse curse = Curses.curseOf(instance);
            if (curse != null) curses.put(curse, instance.getAmplifier());
        }
        if (!curses.isEmpty()) KEPT.put(event.getEntity().getUUID(), curses);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Map<Curse, Integer> curses = KEPT.remove(event.getEntity().getUUID());
        if (curses == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        curses.forEach((curse, amplifier) -> Curses.apply(player, curse, amplifier));
    }
}
