package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.EffectCures;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Putting a curse on a player. A curse instance never carries the totem cure, so a later pop (which
 * clears effects a totem cures, like vanilla) leaves it alone. That missing cure is also how a curse is
 * told apart from, say, a Weakness potion. A curse put on by holding a totem lasts forever and
 * {@link HeldCurses} takes it off again; every other curse has a timer.
 */
public final class Curses {

    private Curses() {}

    /**
     * A stack of {@code count} totems of {@code curse} just saved you: its after-save curse for its full
     * time, at the after-save level that stack gives or any higher level already on you, and protection
     * from dying of it for that time.
     */
    public static void pop(ServerPlayer player, Curse curse, int count) {
        if (curse.effect() == null) return;
        MobEffectInstance current = player.getEffect(curse.effect());
        int amplifier = Math.max(curse.afterSaveAmplifier(count), current == null ? 0 : current.getAmplifier());
        apply(player, curse, amplifier, curse.afterSaveTicks());
        markSaved(player, curse, curse.afterSaveTicks());
    }

    /** {@code curse} can't kill you for the next {@code ticks} (see {@link SavedCurseShield}). */
    public static void markSaved(ServerPlayer player, Curse curse, int ticks) {
        Map<String, Long> saved = new HashMap<>(player.getData(TotemRegistries.SAVED_UNTIL));
        saved.put(curse.name(), player.level().getGameTime() + ticks);
        player.setData(TotemRegistries.SAVED_UNTIL, Map.copyOf(saved));
    }

    /** Whether {@code curse} is still in the time after a save, when it can't kill you. */
    public static boolean isSaved(ServerPlayer player, Curse curse) {
        Long until = player.getData(TotemRegistries.SAVED_UNTIL).get(curse.name());
        return until != null && until > player.level().getGameTime();
    }

    /**
     * Puts {@code curse} on the player at {@code amplifier} for {@code duration} ticks
     * ({@link MobEffectInstance#INFINITE_DURATION} while held), plus its form if it has one. A Rage or
     * Lonely timer starts afresh when the curse is new to you.
     */
    public static void apply(ServerPlayer player, Curse curse, int amplifier, int duration) {
        if (curse.effect() == null) return;
        if (!player.hasEffect(curse.effect())) CurseTimers.restart(player, curse);
        // showIcon true, visible particles off: a curse is a status, not a potion cloud.
        MobEffectInstance instance = new MobEffectInstance(curse.effect(), duration, amplifier, false, false, true);
        instance.getCures().remove(EffectCures.PROTECTED_BY_TOTEM);
        // force: replaces a running instance outright, so its level and timer are exactly these.
        player.forceAddEffect(instance, null);
        if (curse == Curse.MORPHING) Morphs.startRandomMorph(player);
        if (curse == Curse.ZOMBIFIED) Morphs.setForm(player, EntityType.ZOMBIE);
    }

    /** The curse this effect instance is, or null when it is not one (no curse, or a totem would clear it). */
    @Nullable
    public static Curse curseOf(MobEffectInstance instance) {
        if (instance.getCures().contains(EffectCures.PROTECTED_BY_TOTEM)) return null;
        return Curse.forEffect(instance.getEffect());
    }
}
