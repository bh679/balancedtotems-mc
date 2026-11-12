package games.brennan.balancedtotems;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurseTest {

    @Test
    void durationsMatchTheDesign() {
        assertEquals(20 * 60 * 20, Curse.WEAKNESS.durationTicks());
        assertEquals(20 * 60 * 20, Curse.FATIGUE.durationTicks());
        assertEquals(20 * 60 * 20, Curse.HEARTBREAK.durationTicks());
        assertEquals(30 * 20, Curse.BLINDNESS.durationTicks());
        assertEquals(30 * 20, Curse.MORPHING.durationTicks());
        assertEquals(10 * 60 * 20, Curse.ZOMBIFIED.durationTicks());
        assertEquals(15 * 60 * 20, Curse.RAMPAGE.durationTicks());
        assertEquals(10 * 60 * 20, Curse.LONELINESS.durationTicks());
        assertEquals(20 * 60 * 20, Curse.PACIFISM.durationTicks());
        assertEquals(20 * 60 * 20, Curse.MELON.durationTicks());
        assertEquals(20 * 60 * 20, Curse.RESTRAINT.durationTicks());
    }

    /** Which death each narrow totem saves from needs the game's registries: see the GameTest of the same name. */
    @Test
    void onlyVoidAndFeatherLeaveNoCurse() {
        for (Curse curse : Curse.values()) {
            boolean narrow = curse == Curse.VOID || curse == Curse.FEATHER;
            assertEquals(narrow, !curse.leavesCurse(), curse.name() + " should leave a curse unless it is narrow");
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
