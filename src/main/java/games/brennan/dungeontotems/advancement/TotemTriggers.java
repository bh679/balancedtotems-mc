package games.brennan.dungeontotems.advancement;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** The advancement triggers of the Dungeon Totems tab ({@code data/dungeontotems/advancement/}). */
public final class TotemTriggers {

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, DungeonTotems.MOD_ID);

    /** A dungeon totem saved you: {@code curse} is its curse id ({@code weakness}, {@code void}...). */
    public static final Supplier<KeyTrigger> TOTEM_POPPED = TRIGGERS.register("totem_popped", () -> new KeyTrigger("curse"));
    /** You lived through a curse to its end, or lifted it ({@link games.brennan.dungeontotems.curse.CurseTrials}). */
    public static final Supplier<KeyTrigger> CURSE_SURVIVED = TRIGGERS.register("curse_survived", () -> new KeyTrigger("curse"));
    /** A Totem of Morphing made you {@code form} (an entity type id). */
    public static final Supplier<KeyTrigger> BECAME_FORM = TRIGGERS.register("became_form", () -> new KeyTrigger("form"));

    private TotemTriggers() {}

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }
}
