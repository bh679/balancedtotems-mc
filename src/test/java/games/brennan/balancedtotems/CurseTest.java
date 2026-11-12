package games.brennan.balancedtotems;

import net.minecraft.world.damagesource.DamageTypes;
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
    }

    @Test
    void onlyVoidAndFeatherSaveFromOneThing() {
        for (Curse curse : Curse.values()) {
            boolean narrow = curse == Curse.VOID || curse == Curse.FEATHER;
            assertEquals(narrow, curse.onlySaves() != null, curse.name());
            assertEquals(narrow, curse.effect() == null, curse.name() + " should leave a curse unless it is narrow");
        }
        assertEquals(DamageTypes.FELL_OUT_OF_WORLD, Curse.VOID.onlySaves());
        assertEquals(DamageTypes.FALL, Curse.FEATHER.onlySaves());
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
