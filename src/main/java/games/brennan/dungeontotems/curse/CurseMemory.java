package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.Curse;
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
 * A curse a totem left you after saving you follows you through death: it comes back on respawn at the
 * same level with its full after-save time. Curses from holding a totem, or from letting go of one, do
 * not. A deadline curse that killed you by running out is already gone, so it stays gone. Read on the
 * clone (the dead body still holds its effects), applied once the new body is in the world.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class CurseMemory {

    private static final Map<UUID, Map<Curse, Integer>> KEPT = new ConcurrentHashMap<>();

    private CurseMemory() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Map<Curse, Integer> curses = new EnumMap<>(Curse.class);
        for (MobEffectInstance instance : event.getOriginal().getActiveEffects()) {
            Curse curse = Curses.curseOf(instance);
            if (curse != null && event.getOriginal() instanceof ServerPlayer dead && Curses.isSaved(dead, curse)) {
                curses.put(curse, instance.getAmplifier());
            }
        }
        if (!curses.isEmpty()) KEPT.put(event.getEntity().getUUID(), curses);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Map<Curse, Integer> curses = KEPT.remove(event.getEntity().getUUID());
        if (curses == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        curses.forEach((curse, amplifier) -> {
            Curses.apply(player, curse, amplifier, curse.afterSaveTicks());
            Curses.markSaved(player, curse, curse.afterSaveTicks());
        });
    }
}
