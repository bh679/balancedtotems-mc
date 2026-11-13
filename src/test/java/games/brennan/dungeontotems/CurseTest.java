package games.brennan.dungeontotems;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurseTest {

    @Test
    void afterSaveTimesMatchTheDesign() {
        int fourMinutes = 4 * 60 * 20;
        for (Curse curse : new Curse[] {Curse.WEAKNESS, Curse.FATIGUE, Curse.HEARTBREAK, Curse.HUNGER, Curse.SLOWNESS,
                Curse.PACIFISM, Curse.RESTRAINT, Curse.BREATHLESSNESS, Curse.INSOMNIA}) {
            assertEquals(fourMinutes, curse.afterSaveTicks(), curse.name());
        }
        assertEquals(10 * 60 * 20, Curse.MELON_DIET.afterSaveTicks());
        assertEquals(5 * 60 * 20, Curse.RAGE.afterSaveTicks());
        assertEquals(5 * 60 * 20, Curse.LONELINESS.afterSaveTicks());
        assertEquals(30 * 20, Curse.BLINDNESS.afterSaveTicks());
        assertEquals(30 * 20, Curse.MORPHING.afterSaveTicks());
        assertEquals(10 * 60 * 20, Curse.ZOMBIFIED.afterSaveTicks());
    }

    @Test
    void onlyRageAndLonelyStayAfterYouLetGo() {
        for (Curse curse : Curse.values()) {
            boolean lingers = curse == Curse.RAGE || curse == Curse.LONELINESS;
            assertEquals(lingers, curse.letGoTicks() > 0, curse.name());
        }
        assertEquals(60 * 20, Curse.RAGE.letGoTicks());
    }

    @Test
    void onlyLevelledCursesStack() {
        for (Curse curse : Curse.values()) {
            int expected = switch (curse) {
                case WEAKNESS -> 2;
                case FATIGUE, HEARTBREAK, HUNGER, SLOWNESS, RAGE, LONELINESS -> 3;
                default -> 1;
            };
            assertEquals(expected, curse.maxStack(), curse.name());
            assertEquals(expected > 1, curse.isStackable(), curse.name());
        }
    }

    @Test
    void eachTotemInAStackAddsALevelUpToItsLimit() {
        for (Curse curse : Curse.values()) {
            for (int count = 1; count <= curse.maxStack(); count++) {
                assertEquals(curse.heldAmplifier() + count - 1, curse.heldAmplifier(count), curse.name() + " x" + count);
            }
            assertEquals(curse.heldAmplifier(), curse.heldAmplifier(1), curse.name());
            assertEquals(curse.afterSaveAmplifier(), curse.afterSaveAmplifier(1), curse.name());
        }
        assertEquals(1, Curse.WEAKNESS.heldAmplifier(5));
    }

    @Test
    void rageAndLonelyGoOneLevelUpAfterASaveUpToThree() {
        for (Curse curse : new Curse[] {Curse.RAGE, Curse.LONELINESS}) {
            assertEquals(1, curse.afterSaveAmplifier(1), curse.name());
            assertEquals(2, curse.afterSaveAmplifier(2), curse.name());
            assertEquals(2, curse.afterSaveAmplifier(3), curse.name());
        }
        assertEquals(2, Curse.HEARTBREAK.afterSaveAmplifier(3));
    }

    @Test
    void narrowTotemsSaveFromOneThingAndLeaveNoCurse() {
        for (Curse curse : Curse.values()) {
            boolean narrow = curse == Curse.VOID || curse == Curse.FEATHER || curse == Curse.BOOM || curse == Curse.FIRE;
            assertEquals(narrow, curse.isNarrow(), curse.name());
            assertEquals(narrow, curse.effect() == null, curse.name() + " should leave a curse unless it is narrow");
        }
        assertTrue(Curse.FIRE.givesFireResistance());
        assertFalse(Curse.BOOM.givesFireResistance());
    }

    @Test
    void onlyBlindnessMorphingAndZombifiedWaitForTheSave() {
        for (Curse curse : Curse.values()) {
            boolean after = curse == Curse.BLINDNESS || curse == Curse.MORPHING || curse == Curse.ZOMBIFIED;
            assertEquals(after, curse.kind() == Curse.Kind.AFTER, curse.name());
        }
    }

    @Test
    void everyTotemHasItsOwnItemName() {
        Set<String> names = new HashSet<>();
        for (Curse curse : Curse.values()) {
            assertTrue(curse.itemName().endsWith("_totem"), curse.itemName());
            assertTrue(names.add(curse.itemName()), "duplicate " + curse.itemName());
        }
        assertFalse(names.isEmpty());
    }
}
