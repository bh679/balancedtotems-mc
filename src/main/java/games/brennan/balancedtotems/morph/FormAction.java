package games.brennan.balancedtotems.morph;

/** Something a form did that its stand-in mob shows on every client. */
public enum FormAction {
    /** A goat lowering its head to ram. */
    RAM,
    /** An iron golem swinging its arms. */
    SWING,
    /** A sheep, cow or horse grazing. */
    GRAZE,
    /** An armadillo rolling into its shell. */
    ROLL_UP,
    /** An armadillo coming back out. */
    UNROLL,
    /** Any other melee hit: the arm swing (zombified piglin, zombie). */
    SWING_ARM,
    /** A camel's dash. */
    DASH,
    /** A cat sitting down (it stands when you move). */
    SIT,
    /** A bat hanging upside down (it lets go when you move). */
    HANG,
    /** An axolotl playing dead, and coming round. */
    PLAY_DEAD,
    WAKE,
    /** A pufferfish puffing up, and deflating. */
    PUFF,
    DEFLATE,
    /** A frog croaking. */
    CROAK,
    /** A frog's tongue. */
    TONGUE,
    /** A sniffer digging. */
    DIG,
    /** A polar bear rearing up to strike. */
    STAND
}
