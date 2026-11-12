package games.brennan.balancedtotems.gametest;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Registers every {@link BtGameTest} method as a 26.x test function plus a test instance on the
 * {@code balancedtotems:empty} structure, named after the method in snake case
 * ({@code balancedtotems:curse_totem_saves_and_curses}). Only when NeoForge has GameTests on
 * (dev runs and the GameTest server), so a normal game's registries never see them.
 */
public final class BtGameTestRegistry {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, BalancedTotems.MOD_ID);

    private record Registered(DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function, int maxTicks) {}

    private static final List<Registered> TESTS = new ArrayList<>();

    private BtGameTestRegistry() {}

    public static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) return;
        Arrays.stream(BalancedTotemsGameTests.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(BtGameTest.class))
            .sorted(Comparator.comparing(Method::getName))
            .forEach(BtGameTestRegistry::registerFunction);
        FUNCTIONS.register(modBus);
        modBus.addListener(BtGameTestRegistry::registerInstances);
    }

    private static void registerFunction(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("@BtGameTest method must be static: " + method);
        }
        var function = FUNCTIONS.register(snakeCase(method.getName()), () -> helper -> invoke(method, helper));
        TESTS.add(new Registered(function, method.getAnnotation(BtGameTest.class).maxTicks()));
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) throw runtime;
            if (e.getCause() instanceof Error error) throw error;
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void registerInstances(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(BalancedTotems.id("default"));
        for (Registered test : TESTS) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD,
                BalancedTotems.id("empty"), test.maxTicks(), 0, true, Rotation.NONE, false, 1, 1, false, 0);
            event.registerTest(test.function().getId(), new FunctionGameTestInstance(test.function().getKey(), data));
        }
    }

    static String snakeCase(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
            .toLowerCase(java.util.Locale.ROOT);
    }
}
