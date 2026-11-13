package games.brennan.dungeontotems.morph;

import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * How fast each land form walks and runs, from its mob. A mob's AI moves it at a multiple of its
 * movement speed: its stroll multiplier when it wanders, its panic (or chase) multiplier when it hurries.
 * A form walks at twice its mob's stroll and sprints at its run (at least a player's sprint over the
 * walk). Mobs ridden by a player (horses, llamas, camels) run at their ridden speed and walk at half of it
 * (their own stroll is a slow amble). A few forms have tuned speeds instead ({@link Gait#tuned}). Only land forms are
 * here, and the axolotl's walk on land; flyers keep {@link MorphStats#speedBonus}, and other water forms
 * can't walk ({@link WaterLife}).
 *
 * <p>Multipliers are vanilla's: {@code WaterAvoidingRandomStrollGoal}, {@code PanicGoal}, the brain mobs'
 * {@code RandomStroll.stroll} and {@code AnimalPanic}. GameTest {@code mobsWalkAtTheirOwnPace} measures
 * the real mobs against this table.</p>
 */
public final class MobGaits {

    /**
     * A mob's walk and run multipliers. {@code ridden} runs at the ridden speed (the mob's movement
     * speed taken as a player's, plus {@code riddenSprintBonus}, a camel's dash). {@code measuredWalk}
     * and {@code measuredRun} (blocks per second) replace the formula for mobs whose own movement code
     * sets their pace: turtles and axolotls slow themselves on land, frogs and rabbits hop.
     * {@code tunedWalk} and {@code tunedRun} (blocks per second) set the form's own walk and sprint where
     * the rule gives something unplayable (a fox's 19 b/s panic, a polar bear's 5.5 b/s amble).
     */
    public record Gait(double walk, double run, boolean ridden, double riddenSprintBonus, double measuredWalk, double measuredRun,
                       double tunedWalk, double tunedRun) {
        static Gait of(double walk, double run) {
            return new Gait(walk, run, false, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        static Gait ridden(double walk, double sprintBonus) {
            return new Gait(walk, 0.0, true, sprintBonus, 0.0, 0.0, 0.0, 0.0);
        }

        static Gait measured(double walk, double run, double walkBlocksPerSecond, double runBlocksPerSecond) {
            return new Gait(walk, run, false, 0.0, walkBlocksPerSecond, runBlocksPerSecond, 0.0, 0.0);
        }

        /** The mob's own stroll and run, but the form walks and sprints at these blocks per second. */
        Gait tuned(double walkBlocksPerSecond, double runBlocksPerSecond) {
            return new Gait(walk, run, ridden, riddenSprintBonus, measuredWalk, measuredRun, walkBlocksPerSecond, runBlocksPerSecond);
        }
    }

    /** A player's walk (movement speed 0.1) covers this much ground a second. */
    static final double PLAYER_WALK_BLOCKS_PER_SECOND = 4.317;
    static final double PLAYER_WALK_SPEED = 0.1;
    /** How much faster than its mob's stroll a form walks. */
    static final double WALK_OVER_STROLL = 2.0;
    /** How fast a ridden mob's form walks against its gallop. */
    static final double RIDDEN_WALK_OVER_GALLOP = 0.5;
    /** A player's sprint is +30% (multiplied total). */
    static final double PLAYER_SPRINT = 1.3;
    /** Movement input: a player's forward impulse is 0.98; a mob's is its own speed. */
    private static final double PLAYER_INPUT = 0.98;

    public static final Map<EntityType<?>, Gait> GAITS = Map.ofEntries(
        Map.entry(EntityType.ARMADILLO, Gait.of(1.0, 2.0)),
        // AxolotlAi#SPEED_MULTIPLIER_ON_LAND, slowed again by its move control out of water; it swims at its own pace
        Map.entry(EntityType.AXOLOTL, Gait.measured(0.15, 0.15, 0.24, 0.24)),
        Map.entry(EntityType.CAMEL, Gait.ridden(2.0, 0.1).tuned(2.85, 0.0)), // twice its stroll; it dashes on jump
        Map.entry(EntityType.CAT, Gait.of(0.8, 1.5)),
        Map.entry(EntityType.CHICKEN, Gait.of(1.0, 1.4)),
        Map.entry(EntityType.COW, Gait.of(1.0, 2.0)),
        Map.entry(EntityType.DONKEY, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.FOX, Gait.of(1.0, 2.2).tuned(4.0, 7.0)),
        Map.entry(EntityType.FROG, Gait.measured(1.0, 2.0, 0.44, 1.76)),
        Map.entry(EntityType.GOAT, Gait.of(1.0, 2.0)),
        Map.entry(EntityType.HORSE, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.IRON_GOLEM, Gait.of(0.6, 1.0)),
        Map.entry(EntityType.LLAMA, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.MOOSHROOM, Gait.of(1.0, 2.0)),
        Map.entry(EntityType.MULE, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.OCELOT, Gait.of(0.8, 1.33)),
        Map.entry(EntityType.PANDA, Gait.of(1.0, 2.0)),
        Map.entry(EntityType.PIG, Gait.of(1.0, 1.25)),
        Map.entry(EntityType.POLAR_BEAR, Gait.of(1.0, 2.0).tuned(3.3, 4.3)),
        Map.entry(EntityType.RABBIT, Gait.measured(0.6, 2.2, 0.68, 5.55)),
        Map.entry(EntityType.SHEEP, Gait.of(1.0, 1.25)),
        Map.entry(EntityType.SKELETON_HORSE, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.SNIFFER, Gait.of(1.0, 2.0)),
        Map.entry(EntityType.SNOW_GOLEM, Gait.of(1.0, 1.25)),
        Map.entry(EntityType.STRIDER, Gait.of(1.0, 1.65)), // on lava; off it, FormTraits slows it as vanilla does
        Map.entry(EntityType.TRADER_LLAMA, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.TURTLE, Gait.measured(1.0, 1.2, 0.29, 0.34)),
        Map.entry(EntityType.WOLF, Gait.of(1.0, 1.5).tuned(4.0, 7.0)),
        Map.entry(EntityType.ZOMBIE_HORSE, Gait.ridden(0.7, 0.0)),
        Map.entry(EntityType.ZOMBIFIED_PIGLIN, Gait.of(1.0, 1.0).tuned(3.0, 3.9)));

    private MobGaits() {}

    @Nullable
    public static Gait of(@Nullable EntityType<?> form) {
        return form == null ? null : GAITS.get(form);
    }

    /** The player movement speed of the mob's own stroll. */
    public static double strollSpeed(Gait gait, double mobSpeed) {
        if (gait.measuredWalk() > 0.0) return fromBlocksPerSecond(gait.measuredWalk());
        return likeAMob(mobSpeed * gait.walk());
    }

    /** The player movement speed of the mob's own run (its panic or chase; a ridden mob's ridden speed). */
    public static double mobRunSpeed(Gait gait, double mobSpeed) {
        if (gait.ridden()) return mobSpeed + gait.riddenSprintBonus(); // a ridden mob moves on the player's input
        if (gait.measuredRun() > 0.0) return fromBlocksPerSecond(gait.measuredRun());
        return likeAMob(mobSpeed * gait.run());
    }

    /**
     * A form walks at twice its mob's stroll: a mob's amble is too slow to play. A ridden mob walks at half
     * its gallop, and a tuned form at its tuned speed.
     */
    public static double walkSpeed(Gait gait, double mobSpeed) {
        if (gait.tunedWalk() > 0.0) return fromBlocksPerSecond(gait.tunedWalk());
        if (gait.ridden()) return mobRunSpeed(gait, mobSpeed) * RIDDEN_WALK_OVER_GALLOP;
        return strollSpeed(gait, mobSpeed) * WALK_OVER_STROLL;
    }

    /** A form sprints at its mob's run, and always at least a player's sprint over its walk. */
    public static double runSpeed(Gait gait, double mobSpeed) {
        if (gait.tunedRun() > 0.0) return Math.max(fromBlocksPerSecond(gait.tunedRun()), walkSpeed(gait, mobSpeed) * PLAYER_SPRINT);
        return Math.max(mobRunSpeed(gait, mobSpeed), walkSpeed(gait, mobSpeed) * PLAYER_SPRINT);
    }

    /**
     * {@code Mob#setSpeed} also sets its forward input to that speed, so a mob moving at {@code s}
     * accelerates by {@code s * s} where a player at {@code m} accelerates by {@code m * 0.98}.
     */
    public static double likeAMob(double mobSpeed) {
        return mobSpeed * mobSpeed / PLAYER_INPUT;
    }

    public static double fromBlocksPerSecond(double blocksPerSecond) {
        return PLAYER_WALK_SPEED * blocksPerSecond / PLAYER_WALK_BLOCKS_PER_SECOND;
    }

    public static double toBlocksPerSecond(double playerSpeed) {
        return playerSpeed / PLAYER_WALK_SPEED * PLAYER_WALK_BLOCKS_PER_SECOND;
    }
}
