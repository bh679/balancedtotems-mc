package games.brennan.balancedtotems.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static void (GameTestHelper)} method in {@link BalancedTotemsGameTests} as a test.
 * 26.x dropped vanilla's {@code @GameTest}; {@link BtGameTestRegistry} registers these instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BtGameTest {
    /** Ticks before the test times out (vanilla's old default). */
    int maxTicks() default 100;
}
