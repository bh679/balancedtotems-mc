package games.brennan.balancedtotems;

import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** What each totem leaves behind after it saves you. Durations are in ticks. */
public enum Curse {
    WEAKNESS("weakness_totem", () -> MobEffects.WEAKNESS, Ticks.MINUTE * 20),
    FATIGUE("fatigue_totem", () -> MobEffects.DIG_SLOWDOWN, Ticks.MINUTE * 20),
    HEARTBREAK("heartbreak_totem", () -> BtRegistries.HEARTBREAK, Ticks.MINUTE * 20),
    BLINDNESS("blindness_totem", () -> MobEffects.BLINDNESS, Ticks.SECOND * 20),
    MORPHING("morphing_totem", () -> BtRegistries.MORPHED, Ticks.SECOND * 20),
    ZOMBIFIED("zombified_totem", () -> BtRegistries.ZOMBIFIED, Ticks.MINUTE * 10),
    RAMPAGE("rampage_totem", () -> BtRegistries.RAMPAGE, Ticks.MINUTE * 15),
    /** Saves you from the void and lethal falls only, and leaves no curse. */
    VOID("void_totem", null, 0);

    private final String itemName;
    @Nullable private final Supplier<Holder<MobEffect>> effect;
    private final int durationTicks;

    Curse(String itemName, @Nullable Supplier<Holder<MobEffect>> effect, int durationTicks) {
        this.itemName = itemName;
        this.effect = effect;
        this.durationTicks = durationTicks;
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

    public boolean savesFromVoidOnly() {
        return this == VOID;
    }

    /** Tick constants, kept here so the table above reads in minutes and seconds. */
    public static final class Ticks {
        public static final int SECOND = 20;
        public static final int MINUTE = 60 * SECOND;

        private Ticks() {}
    }
}
