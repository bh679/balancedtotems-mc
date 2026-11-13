package games.brennan.dungeontotems;

import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * What each totem does to you. Amplifiers are levels (0 is level I); times are in ticks.
 * <ul>
 *   <li>{@link Kind#HELD}: the curse is on you while you hold the totem, and for {@code afterSaveTicks}
 *       once it has saved you. Rage and Lonely also stay {@code letGoTicks} after you let go. Some stack
 *       (see {@link #maxStack()}): each totem in the stack adds a level.</li>
 *   <li>{@link Kind#AFTER}: nothing while held; the curse starts when it saves you.</li>
 *   <li>{@link Kind#NARROW}: saves you from one kind of death only, and leaves no curse.</li>
 * </ul>
 */
public enum Curse {
    WEAKNESS("weakness_totem", Kind.HELD, () -> MobEffects.WEAKNESS, 0, 0, Ticks.MINUTE * 4),
    FATIGUE("fatigue_totem", Kind.HELD, () -> MobEffects.DIG_SLOWDOWN, 0, 0, Ticks.MINUTE * 4),
    HEARTBREAK("heartbreak_totem", Kind.HELD, () -> TotemRegistries.HEARTBREAK, 0, 0, Ticks.MINUTE * 4),
    HUNGER("hunger_totem", Kind.HELD, () -> MobEffects.HUNGER, 0, 0, Ticks.MINUTE * 4),
    SLOWNESS("slowness_totem", Kind.HELD, () -> MobEffects.MOVEMENT_SLOWDOWN, 0, 0, Ticks.MINUTE * 4),
    BLINDNESS("blindness_totem", Kind.AFTER, () -> MobEffects.BLINDNESS, 0, 0, Ticks.SECOND * 30),
    MORPHING("morphing_totem", Kind.AFTER, () -> TotemRegistries.MORPHED, 0, 0, Ticks.SECOND * 30),
    ZOMBIFIED("zombification_totem", Kind.AFTER, () -> TotemRegistries.ZOMBIFIED, 0, 0, Ticks.MINUTE * 10),
    /** Hit something within the timer (see {@code curse/CurseTimers}). */
    RAGE("rage_totem", Kind.HELD, () -> TotemRegistries.RAGE, 0, 1, Ticks.MINUTE * 5, Ticks.MINUTE),
    /** Give a gift within the timer (see {@code curse/CurseTimers}). */
    LONELINESS("loneliness_totem", Kind.HELD, () -> TotemRegistries.LONELY, 0, 1, Ticks.MINUTE * 5, Ticks.MINUTE),
    /** Hurt anything and the held totem pops; after a save, you drop to half a heart. */
    PACIFISM("pacifism_totem", Kind.HELD, () -> TotemRegistries.VOW_OF_PEACE, 0, 0, Ticks.MINUTE * 4),
    /** Eat anything but melon and the held totem pops; after a save, you drop to half a heart. */
    MELON_DIET("melon_diet_totem", Kind.HELD, () -> TotemRegistries.MELON_DIET, 0, 0, Ticks.MINUTE * 10),
    /** Open a chest, barrel or ender chest and the held totem pops; after a save, you drop to half a heart. */
    RESTRAINT("restraint_totem", Kind.HELD, () -> TotemRegistries.RESTRAINED, 0, 0, Ticks.MINUTE * 4),
    /** You lose air even out of water (see {@code curse/Breathless}). */
    BREATHLESSNESS("breathlessness_totem", Kind.HELD, () -> TotemRegistries.BREATHLESS, 0, 0, Ticks.MINUTE * 4),
    /** Phantoms come for you, by day as well (see {@code curse/InsomniaPhantoms}). */
    INSOMNIA("insomnia_totem", Kind.HELD, () -> TotemRegistries.INSOMNIA, 0, 0, Ticks.MINUTE * 4),
    /** Saves you only from falling out of the world. */
    VOID("void_totem", Kind.NARROW, null, 0, 0, 0),
    /** Saves you only from a deadly fall. */
    FEATHER("feather_totem", Kind.NARROW, null, 0, 0, 0),
    /** Saves you only from explosions. */
    BOOM("boom_totem", Kind.NARROW, null, 0, 0, 0),
    /** Saves you only from fire and lava, and keeps the pop's Fire Resistance. */
    FIRE("fire_totem", Kind.NARROW, null, 0, 0, 0);

    public enum Kind { HELD, AFTER, NARROW }

    private final String itemName;
    private final Kind kind;
    @Nullable private final Supplier<Holder<MobEffect>> effect;
    private final int heldAmplifier;
    private final int afterSaveAmplifier;
    private final int afterSaveTicks;
    private final int letGoTicks;

    Curse(String itemName, Kind kind, @Nullable Supplier<Holder<MobEffect>> effect,
          int heldAmplifier, int afterSaveAmplifier, int afterSaveTicks) {
        this(itemName, kind, effect, heldAmplifier, afterSaveAmplifier, afterSaveTicks, 0);
    }

    Curse(String itemName, Kind kind, @Nullable Supplier<Holder<MobEffect>> effect,
          int heldAmplifier, int afterSaveAmplifier, int afterSaveTicks, int letGoTicks) {
        this.itemName = itemName;
        this.kind = kind;
        this.effect = effect;
        this.heldAmplifier = heldAmplifier;
        this.afterSaveAmplifier = afterSaveAmplifier;
        this.afterSaveTicks = afterSaveTicks;
        this.letGoTicks = letGoTicks;
    }

    public String itemName() {
        return itemName;
    }

    public Kind kind() {
        return kind;
    }

    /** The curse effect, or null for a narrow totem. */
    @Nullable
    public Holder<MobEffect> effect() {
        return effect == null ? null : effect.get();
    }

    public int heldAmplifier() {
        return heldAmplifier;
    }

    public int afterSaveAmplifier() {
        return afterSaveAmplifier;
    }

    /** The held level a stack of {@code count} gives: a level per totem, up to {@link #maxStack()}. */
    public int heldAmplifier(int count) {
        return stacked(heldAmplifier, count);
    }

    /** The after-save level when a stack of {@code count} saves you (Rage and Lonely: one above held, up to III). */
    public int afterSaveAmplifier(int count) {
        return stacked(afterSaveAmplifier, count);
    }

    private int stacked(int amplifier, int count) {
        return Math.min(amplifier + Math.max(count, 1) - 1, Math.max(amplifier, maxStack() - 1));
    }

    /**
     * How many fit in a stack: as many as the curse has levels. Weakness II already leaves a sword
     * near useless; Rage and Lonely have three timers (60, 40, 30 s); the rest have no levels.
     */
    public int maxStack() {
        return switch (this) {
            case WEAKNESS -> 2;
            case FATIGUE, HEARTBREAK, HUNGER, SLOWNESS, RAGE, LONELINESS -> 3;
            default -> 1;
        };
    }

    public boolean isStackable() {
        return maxStack() > 1;
    }

    public int afterSaveTicks() {
        return afterSaveTicks;
    }

    /** How long the curse stays once you let go of the totem, at the level you held; 0 when it ends at once. */
    public int letGoTicks() {
        return letGoTicks;
    }

    public boolean isHeld() {
        return kind == Kind.HELD;
    }

    public boolean isNarrow() {
        return kind == Kind.NARROW;
    }

    /** Its id in advancements: {@code weakness}, {@code melon_diet}, {@code void}... */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** A rule curse: break the rule while holding the totem and it pops. */
    public boolean isRule() {
        return this == PACIFISM || this == MELON_DIET || this == RESTRAINT;
    }

    /** Whether the pop gives Fire Resistance: every totem but the narrow ones, except Fire. */
    public boolean givesFireResistance() {
        return !isNarrow() || this == FIRE;
    }

    /** The curse whose effect this is, or null. */
    @Nullable
    public static Curse forEffect(Holder<MobEffect> effect) {
        for (Curse curse : values()) {
            if (curse.effect != null && curse.effect().is(effect)) return curse;
        }
        return null;
    }

    /** For a narrow totem, whether this is the death it saves you from. */
    public boolean narrowlySaves(DamageSource source) {
        return switch (this) {
            case VOID -> source.is(DamageTypes.FELL_OUT_OF_WORLD);
            case FEATHER -> source.is(DamageTypes.FALL);
            case BOOM -> source.is(DamageTypeTags.IS_EXPLOSION);
            case FIRE -> source.is(DamageTypeTags.IS_FIRE);
            default -> false;
        };
    }

    /** Tick constants, kept here so the table above reads in minutes and seconds. */
    public static final class Ticks {
        public static final int SECOND = 20;
        public static final int MINUTE = 60 * SECOND;

        private Ticks() {}
    }
}
