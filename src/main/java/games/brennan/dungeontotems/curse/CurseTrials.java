package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.advancement.TotemTriggers;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether you lived through a curse, for the advancements ({@link TotemTriggers#CURSE_SURVIVED}).
 * <ul>
 *   <li>A pop starts a trial for its curse, kept on the player across relogs. Dying for real ends every
 *       trial: a curse that comes back after death doesn't count.</li>
 *   <li>A curse is lived through when its after-save time ({@link Curses#isSaved}) is over and you're
 *       alive. That time is the save's own, whatever a held totem or a let-go Rage does to the effect.</li>
 *   <li>Zombified is lived through only when cured ({@link DeadlineCurses#cure}); running out kills you.</li>
 *   <li>The narrow totems leave no curse: being saved is surviving.</li>
 * </ul>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class CurseTrials {

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

    /** From {@link DeadlineCurses#cure}: Zombified was cured in time. */
    static void onLifted(ServerPlayer player, Curse curse) {
        survive(player, curse);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { // a totem's save cancels the event first
            player.setData(TotemRegistries.CURSE_TRIALS, List.of());
        }
    }

    /** Last in the tick, so a deferred death ({@link DeadlineCurses#doom}) has had its chance. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        for (String key : trials(player)) {
            Curse curse = forKey(key);
            if (curse == null) {
                end(player, key); // a curse that no longer exists
            } else if (curse != Curse.ZOMBIFIED && !Curses.isSaved(player, curse)) {
                survive(player, curse);
            }
        }
    }

    private static void survive(ServerPlayer player, Curse curse) {
        if (!trials(player).contains(curse.key())) return;
        end(player, curse.key());
        TotemTriggers.CURSE_SURVIVED.get().trigger(player, curse.key());
    }

    private static void end(ServerPlayer player, String key) {
        List<String> trials = new ArrayList<>(trials(player));
        trials.remove(key);
        player.setData(TotemRegistries.CURSE_TRIALS, List.copyOf(trials));
    }

    private static Curse forKey(String key) {
        for (Curse curse : Curse.values()) {
            if (curse.key().equals(key)) return curse;
        }
        return null;
    }

    private static List<String> trials(ServerPlayer player) {
        return player.getData(TotemRegistries.CURSE_TRIALS);
    }
}
