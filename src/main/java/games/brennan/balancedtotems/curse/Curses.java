package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.registry.BtRegistries;
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

    /** Puts {@code curse} on the player at {@code amplifier} with a full timer, plus its form if it has one. */
    public static void apply(ServerPlayer player, Curse curse, int amplifier) {
        if (curse.effect() == null) return;
        if (curse == Curse.RAMPAGE && !player.hasEffect(BtRegistries.RAMPAGE)) DeadlineCurses.setRampageKills(player, 0);
        // showIcon true, visible particles off: a curse is a status, not a potion cloud.
        MobEffectInstance instance = new MobEffectInstance(curse.effect(), curse.durationTicks(), amplifier, false, false, true);
        instance.getCures().remove(EffectCures.PROTECTED_BY_TOTEM);
        // force: replaces a running instance outright, so a level up also resets the timer.
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
