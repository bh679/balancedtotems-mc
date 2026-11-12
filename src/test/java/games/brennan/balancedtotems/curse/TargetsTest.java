package games.brennan.balancedtotems.curse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetsTest {

    @Test
    void villagersStandInOnlyWithoutPlayerMobAndDungeonTrain() {
        assertTrue(Targets.villagersStandIn(false, false));
        assertFalse(Targets.villagersStandIn(true, false), "PlayerMobs are the targets");
        assertFalse(Targets.villagersStandIn(false, true), "Dungeon Train's villagers never count");
        assertFalse(Targets.villagersStandIn(true, true));
    }
}
