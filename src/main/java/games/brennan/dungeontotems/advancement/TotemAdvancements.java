package games.brennan.dungeontotems.advancement;

import com.mojang.serialization.MapCodec;
import games.brennan.dungeontotems.DungeonTotems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Whether the advancement tab is out yet. Until it is, every file in {@code data/dungeontotems/advancement/}
 * fails its load condition ({@code dungeontotems:advancements_launched}), so the game never loads it: no
 * tab, no toasts, nothing to grant, nothing for another mod's capstone to count. Triggers still fire into
 * nothing. Launching is a release that sets {@link #LAUNCHED}.
 */
public final class TotemAdvancements {

    /** Set in a release to turn the tab on. */
    public static final boolean LAUNCHED = false;
    /** Dev runs only ({@code build.gradle}): load the tab before launch, for testing. */
    static final String DEV_PROPERTY = "dungeontotems.advancements";

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
        DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, DungeonTotems.MOD_ID);

    private TotemAdvancements() {}

    public static boolean launched() {
        return isLaunched(LAUNCHED, System.getProperty(DEV_PROPERTY));
    }

    /** Launched, or loaded early by a dev run's property. */
    public static boolean isLaunched(boolean launched, String devProperty) {
        return launched || Boolean.parseBoolean(devProperty);
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register("advancements_launched", () -> Launched.CODEC);
        CONDITIONS.register(modBus);
    }

    /** {@code {"type": "dungeontotems:advancements_launched"}}: true once the tab is out. */
    public static final class Launched implements ICondition {
        static final Launched INSTANCE = new Launched();
        static final MapCodec<Launched> CODEC = MapCodec.unit(INSTANCE).stable();

        private Launched() {}

        @Override
        public boolean test(IContext context) {
            return launched();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
