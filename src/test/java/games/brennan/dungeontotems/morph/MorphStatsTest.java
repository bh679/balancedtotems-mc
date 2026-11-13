package games.brennan.dungeontotems.morph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphStatsTest {

    private static final double EPS = 1e-9;

    @Test
    void maxHealthFollowsTheMobWithinBounds() {
        assertEquals(-10.0, MorphStats.maxHealthDelta(10.0), EPS);   // cow: 5 hearts
        assertEquals(-14.0, MorphStats.maxHealthDelta(4.0), EPS);    // chicken floors at 6
        assertEquals(20.0, MorphStats.maxHealthDelta(100.0), EPS);   // iron golem caps at 40
    }

    @Test
    void morphNeverLeavesYouWithoutHealth() {
        for (double mob = 0; mob <= 200; mob += 0.5) {
            assertTrue(MorphStats.PLAYER_MAX_HEALTH + MorphStats.maxHealthDelta(mob) >= 6.0);
        }
    }

    @Test
    void speedIsARatioAgainstAZombieLikeWalk() {
        assertEquals(0.0, MorphStats.speedBonus(0.25), EPS);   // pig, polar bear, golem
        assertEquals(-0.2, MorphStats.speedBonus(0.2), EPS);   // cow, goat
        assertEquals(0.2, MorphStats.speedBonus(0.3), EPS);    // fox, wolf, rabbit
        assertEquals(-0.5, MorphStats.speedBonus(0.01), EPS);  // never slower than half
        assertEquals(0.5, MorphStats.speedBonus(5.0), EPS);    // never faster than 1.5x
    }

    @Test
    void attackKeepsTheFistAndCapsBigHitters() {
        assertEquals(0.0, MorphStats.attackDelta(0.0), EPS);
        assertEquals(3.0, MorphStats.attackDelta(4.0), EPS);   // wolf
        assertEquals(9.0, MorphStats.attackDelta(15.0), EPS);  // iron golem capped at 10
    }

    @Test
    void jumpAndStepStayInRange() {
        assertEquals(0.0, MorphStats.jumpBonus(0.42), EPS);
        assertEquals(0.8, MorphStats.jumpBonus(2.0), EPS);
        assertEquals(0.0, MorphStats.stepDelta(0.6), EPS);
        assertEquals(0.4, MorphStats.stepDelta(1.0), EPS);    // horse steps a full block
    }
}
