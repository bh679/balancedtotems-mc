package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.advancement.TotemTriggers;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether you lived through a curse, for the advancements ({@link TotemTriggers#CURSE_SURVIVED}).
 * <ul>
 *   <li>A pop starts a trial for its curse, kept on the player across relogs. Dying for real ends every
 *       trial: a curse that comes back after death doesn't count.</li>
 *   <li>Timed curses and vows are survived when they run out with you alive. That is checked at the end of
 *       the tick, after a broken vow's deferred death ({@link DeadlineCurses#doom}).</li>
 *   <li>Zombified, Rage and Loneliness are survived when lifted ({@link DeadlineCurses#cure}); running out
 *       kills you, so it fails.</li>
 *   <li>The Void, Feather and Boom totems leave no curse: being saved is surviving.</li>
 * </ul>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class CurseTrials {

    /** Curses that ran out this tick, waiting to see whether you're still alive at the end of it. */
    private static final Map<UUID, Set<Curse>> RAN_OUT = new ConcurrentHashMap<>();

    private CurseTrials() {}

    /** From {@code TotemPop}: a dungeon totem saved the player. */
    public static void onPop(ServerPlayer player, Curse curse) {
        TotemTriggers.TOTEM_POPPED.get().trigger(player, curse.key());
        if (curse.isNarrow()) {
            TotemTriggers.CURSE_SURVIVED.get().trigger(player, curse.key());
        } else if (!trials(player).contains(curse.key())) {
            List<String> trials = new ArrayList<>(trials(player));
            trials.add(curse.key());
            player.setData(TotemRegistries.CURSE_TRIALS, List.copyOf(trials));
        }
    }

    /** From {@link DeadlineCurses#cure}: a deadline curse was lifted in time. */
    static void onLifted(ServerPlayer player, Curse curse) {
        survive(player, curse);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { // a totem's save cancels the event first
            player.setData(TotemRegistries.CURSE_TRIALS, List.of());
            RAN_OUT.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        Curse curse = Curses.curseOf(instance);
        if (curse == null || !trials(player).contains(curse.key())) return;
        if (curse.hasDeadline()) end(player, curse); // out of time: it kills you
        else RAN_OUT.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(curse);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Set<Curse> ranOut = RAN_OUT.remove(player.getUUID());
        if (ranOut == null || !player.isAlive()) return;
        ranOut.forEach(curse -> survive(player, curse));
    }

    private static void survive(ServerPlayer player, Curse curse) {
        if (!trials(player).contains(curse.key())) return;
        end(player, curse);
        TotemTriggers.CURSE_SURVIVED.get().trigger(player, curse.key());
    }

    private static void end(ServerPlayer player, Curse curse) {
        List<String> trials = new ArrayList<>(trials(player));
        trials.remove(curse.key());
        player.setData(TotemRegistries.CURSE_TRIALS, List.copyOf(trials));
    }

    private static List<String> trials(ServerPlayer player) {
        return player.getData(TotemRegistries.CURSE_TRIALS);
    }
}
