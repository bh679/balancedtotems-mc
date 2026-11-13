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
        assertEquals(10 * 60 * 20, Curse.MELON.afterSaveTicks());
        assertEquals(5 * 60 * 20, Curse.RAMPAGE.afterSaveTicks());
        assertEquals(5 * 60 * 20, Curse.LONELINESS.afterSaveTicks());
        assertEquals(30 * 20, Curse.BLINDNESS.afterSaveTicks());
        assertEquals(30 * 20, Curse.MORPHING.afterSaveTicks());
        assertEquals(10 * 60 * 20, Curse.ZOMBIFIED.afterSaveTicks());
    }

    @Test
    void onlyRageAndLonelyStayAfterYouLetGo() {
        for (Curse curse : Curse.values()) {
            boolean lingers = curse == Curse.RAMPAGE || curse == Curse.LONELINESS;
            assertEquals(lingers, curse.letGoTicks() > 0, curse.name());
        }
        assertEquals(60 * 20, Curse.RAMPAGE.letGoTicks());
        assertEquals(0, Curse.RAMPAGE.letGoAmplifier());
        assertEquals(0, Curse.LONELINESS.letGoAmplifier());
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
