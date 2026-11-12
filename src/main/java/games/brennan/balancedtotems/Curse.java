package games.brennan.balancedtotems;

import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * What each totem leaves behind after it saves you. Durations are in ticks; the base amplifier is the
 * level a first pop gives (0 is level I). Popping the same totem again raises the level and resets the
 * timer, except Fatigue, Pacifism, Melons and Restraint, which add their duration to the time
 * left instead.
 */
public enum Curse {
    WEAKNESS("weakness_totem", () -> MobEffects.WEAKNESS, Ticks.MINUTE * 20),
    /** Mining Fatigue II: level I is only 30% slower and barely noticed. */
    FATIGUE("fatigue_totem", () -> MobEffects.DIG_SLOWDOWN, Ticks.MINUTE * 20, 1),
    HEARTBREAK("heartbreak_totem", () -> BtRegistries.HEARTBREAK, Ticks.MINUTE * 20),
    BLINDNESS("blindness_totem", () -> MobEffects.BLINDNESS, Ticks.SECOND * 30),
    MORPHING("morphing_totem", () -> BtRegistries.MORPHED, Ticks.SECOND * 30),
    ZOMBIFIED("zombified_totem", () -> BtRegistries.ZOMBIFIED, Ticks.MINUTE * 10),
    RAMPAGE("rampage_totem", () -> BtRegistries.RAMPAGE, Ticks.MINUTE * 15),
    LONELINESS("loneliness_totem", () -> BtRegistries.LONELY, Ticks.MINUTE * 10),
    /** Hurt anything while it lasts and you die. */
    PACIFISM("pacifism_totem", () -> BtRegistries.VOW_OF_PEACE, Ticks.MINUTE * 20),
    /** Eat anything but melon while it lasts and you die. */
    MELON("melon_totem", () -> BtRegistries.MELON_DIET, Ticks.MINUTE * 20),
    /** Open a chest, barrel or ender chest while it lasts and you die. */
    RESTRAINT("restraint_totem", () -> BtRegistries.RESTRAINED, Ticks.MINUTE * 20),
    /** Saves you only from falling out of the world, and leaves no curse. */
    VOID("void_totem", null, 0),
    /** Saves you only from a deadly fall, and leaves no curse. */
    FEATHER("feather_totem", null, 0);

    private final String itemName;
    @Nullable private final Supplier<Holder<MobEffect>> effect;
    private final int durationTicks;
    private final int baseAmplifier;

    Curse(String itemName, @Nullable Supplier<Holder<MobEffect>> effect, int durationTicks) {
        this(itemName, effect, durationTicks, 0);
    }

    Curse(String itemName, @Nullable Supplier<Holder<MobEffect>> effect, int durationTicks, int baseAmplifier) {
        this.itemName = itemName;
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.baseAmplifier = baseAmplifier;
    }

    public String itemName() {
        return itemName;
    }

    /** The curse effect, or null for the Void Totem. */
    @Nullable
    public Holder<MobEffect> effect() {
        return effect == null ? null : effect.get();
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int baseAmplifier() {
        return baseAmplifier;
    }

    /** The curse whose effect this is, or null. */
    @Nullable
    public static Curse forEffect(Holder<MobEffect> effect) {
        for (Curse curse : values()) {
            if (curse.effect != null && curse.effect().is(effect)) return curse;
        }
        return null;
    }

    /** Another pop of this totem adds its duration to the time left rather than a level. */
    public boolean addsTimeNotLevels() {
        return this == FATIGUE || this == PACIFISM || this == MELON || this == RESTRAINT;
    }

    /** The one kind of death this totem saves you from, or null for any death a vanilla totem saves. */
    @Nullable
    public ResourceKey<DamageType> onlySaves() {
        return switch (this) {
            case VOID -> DamageTypes.FELL_OUT_OF_WORLD;
            case FEATHER -> DamageTypes.FALL;
            default -> null;
        };
    }

    /** Tick constants, kept here so the table above reads in minutes and seconds. */
    public static final class Ticks {
        public static final int SECOND = 20;
        public static final int MINUTE = 60 * SECOND;

        private Ticks() {}
    }
}
