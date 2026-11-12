package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.effect.HeartbreakEffect;
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.EffectCures;

import javax.annotation.Nullable;

/**
 * Putting a curse on a player. A curse instance never carries the totem cure, so a later pop (which
 * clears effects a totem cures, like vanilla) leaves it alone: curses stack, and the same curse can
 * level up. That missing cure is also how a curse is told apart from, say, a Weakness potion.
 */
public final class Curses {

    private Curses() {}

    /**
     * A totem of {@code curse} just popped. New curse: its base level and full time. Already cursed:
     * Fatigue adds its time to what is left; Heartbreak at half a heart only refreshes the timer;
     * anything else goes up a level with a fresh timer.
     */
    public static void pop(ServerPlayer player, Curse curse) {
        MobEffectInstance current = player.getEffect(curse.effect());
        if (current == null) {
            apply(player, curse, curse.baseAmplifier(), curse.durationTicks());
        } else if (curse.addsTimeNotLevels()) {
            apply(player, curse, current.getAmplifier(), current.getDuration() + curse.durationTicks());
        } else if (curse == Curse.HEARTBREAK && HeartbreakEffect.atHalfHeart(player)) {
            apply(player, curse, current.getAmplifier(), curse.durationTicks());
        } else {
            apply(player, curse, current.getAmplifier() + 1, curse.durationTicks());
        }
    }

    /** Puts {@code curse} on the player at {@code amplifier} for {@code duration} ticks, plus its form if it has one. */
    public static void apply(ServerPlayer player, Curse curse, int amplifier, int duration) {
        if (curse.effect() == null) return;
        if (Tallies.counted(curse) && !player.hasEffect(curse.effect())) Tallies.reset(player, curse);
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
