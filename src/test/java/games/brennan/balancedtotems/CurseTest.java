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
        assertEquals(20 * 20, Curse.BLINDNESS.durationTicks());
        assertEquals(20 * 20, Curse.MORPHING.durationTicks());
        assertEquals(10 * 60 * 20, Curse.ZOMBIFIED.durationTicks());
        assertEquals(15 * 60 * 20, Curse.RAMPAGE.durationTicks());
    }

    @Test
    void onlyTheVoidTotemIsVoidOnly() {
        for (Curse curse : Curse.values()) {
            assertEquals(curse == Curse.VOID, curse.savesFromVoidOnly(), curse.name());
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
