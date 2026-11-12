package games.brennan.balancedtotems.morph;

/**
 * How a mob's attributes become a morphed player's, as plain numbers so they are testable.
 *
 * <p>Mob and player attribute values aren't on the same scale (a mob's movement speed feeds its AI
 * differently from a player's walk), so speed and jump are taken as a ratio against a typical
 * value, and every result is clamped to keep a 20-second form fun rather than broken.</p>
 */
public final class MorphStats {

    public static final double PLAYER_MAX_HEALTH = 20.0;
    public static final double PLAYER_ATTACK_DAMAGE = 1.0;
    public static final double PLAYER_STEP_HEIGHT = 0.6;

    /** A zombie's 0.23 walks about as fast as a player, so 0.25 reads as "player speed". */
    static final double REFERENCE_MOB_SPEED = 0.25;
    static final double REFERENCE_JUMP = 0.42;

    /** Floor keeps a morph from leaving you on almost no hearts; it can never kill you. */
    static final double MIN_MAX_HEALTH = 6.0;
    static final double MAX_MAX_HEALTH = 40.0;
    static final double MAX_ATTACK_DAMAGE = 10.0;

    private MorphStats() {}

    /** Added to max health (ADD_VALUE). */
    public static double maxHealthDelta(double mobMaxHealth) {
        return clamp(mobMaxHealth, MIN_MAX_HEALTH, MAX_MAX_HEALTH) - PLAYER_MAX_HEALTH;
    }

    /**
     * Multiplier on movement speed, as an ADD_MULTIPLIED_TOTAL amount (result minus one), for flyers and
     * swimmers. Land forms walk at their mob's own pace instead ({@link MobGaits}).
     */
    public static double speedBonus(double mobSpeed) {
        return clamp(mobSpeed / REFERENCE_MOB_SPEED, 0.5, 1.5) - 1.0;
    }

    /** Added to attack damage (ADD_VALUE); a mob with no attack keeps the player's fist. */
    public static double attackDelta(double mobAttackDamage) {
        return clamp(mobAttackDamage, PLAYER_ATTACK_DAMAGE, MAX_ATTACK_DAMAGE) - PLAYER_ATTACK_DAMAGE;
    }

    /** Multiplier on jump strength, as an ADD_MULTIPLIED_TOTAL amount. */
    public static double jumpBonus(double mobJumpStrength) {
        return clamp(mobJumpStrength / REFERENCE_JUMP, 0.8, 1.8) - 1.0;
    }

    /** Added to step height (ADD_VALUE): horses and golems step up full blocks. */
    public static double stepDelta(double mobStepHeight) {
        return clamp(mobStepHeight, PLAYER_STEP_HEIGHT, 1.5) - PLAYER_STEP_HEIGHT;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
