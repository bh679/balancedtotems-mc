package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormAction;
import games.brennan.balancedtotems.morph.FormTraits;
import games.brennan.balancedtotems.morph.MorphFlight;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animates a morph's stand-in mob the way vanilla animates the real one. Each tick the stand-in runs
 * its own {@code tick()} (it is never in the world and its AI never runs on a client, so this only
 * advances its animation code: bat wings, horse tail and stance, sheep grazing, golem swing), then is
 * snapped back onto the player. Moments the mob's AI would cause, like a rabbit hop or a horse rearing
 * on a jump, are started here from what the player does; attacks and grazing arrive from the server.
 */
final class FormAnimator {

    /** How long a horse rears for when it jumps. */
    private static final int HORSE_REAR_TICKS = 10;
    private static final int RAM_TICKS = 10;
    private static final int EAT_TICKS = 20;
    private static final int DASH_TICKS = 20;
    private static final int CROAK_TICKS = 60;
    private static final int TONGUE_TICKS = 10;
    /** {@code LandMoves#DIG_SEED_DELAY}: the sniffer digs until the seed comes up. */
    private static final int DIG_TICKS = 120;
    /** {@code PolarBearMeleeAttackGoal}: how long the bear stays up on its hind legs. */
    private static final int STAND_TICKS = 20;
    /** {@code Armadillo.ArmadilloState} durations: rolling into the ball, and unrolling out of it. */
    private static final int ROLL_UP_TICKS = 10;
    private static final int UNROLL_TICKS = 30;
    /** A bee's sting flip: it tips nose-up until its stinger swings round underneath, then back. */
    private static final int FLIP_TICKS = 10;
    private static final float FLIP_DEGREES = -100.0F;
    /** Any other form's attack: a quick dip forward from its feet. */
    private static final int LUNGE_TICKS = 6;
    private static final float LUNGE_DEGREES = 20.0F;
    /** How long a chicken or parrot flaps for when it attacks. */
    private static final int FLAP_TICKS = 8;
    /** Moving further than this in a tick ends a sit or a hang. */
    private static final double STILL_DISTANCE_SQR = 0.0025;

    private static final Map<UUID, Boolean> WAS_ON_GROUND = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REARING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RAMMING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EATING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DASHING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STANDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FLAPPING = new ConcurrentHashMap<>();
    /** A whole-body tilt the renderer draws (a bee's flip, a lunge). */
    private static final Map<UUID, Tilt> TILTS = new ConcurrentHashMap<>();
    /** A curled armadillo keeps the way it faced when it rolled up. */
    private static final Map<UUID, Float> CURLED_YAW = new ConcurrentHashMap<>();
    /** Armadillos all the way into their ball (not rolling up or out). */
    private static final Set<UUID> CURLED = ConcurrentHashMap.newKeySet();
    /** A pose the mob holds for a while (croak, tongue, dig), and how it ends. */
    private static final Map<UUID, Timed> TIMED = new ConcurrentHashMap<>();
    /** Sitting cats and hanging bats, until the player moves. */
    private static final Set<UUID> RESTING = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();
    /** The last action each stand-in showed (for the self-test). */
    private static final Map<UUID, FormAction> LAST_ACTION = new ConcurrentHashMap<>();

    private FormAnimator() {}

    static void tick(LivingEntity standIn, Player player) {
        UUID id = player.getUUID();
        boolean jumped = WAS_ON_GROUND.getOrDefault(id, true) && !player.onGround() && player.getDeltaMovement().y > 0;
        WAS_ON_GROUND.put(id, player.onGround());

        standIn.setOldPosAndRot();
        follow(standIn, player);
        if (FLAPPING.containsKey(id)) standIn.setOnGround(false); // a chicken's wings beat while it's off the ground
        runOwnTick(standIn);
        follow(standIn, player);
        if (standIn instanceof Armadillo armadillo && armadillo.isScared()) {
            armadillo.walkAnimation.setSpeed(0.0F); // a ball doesn't walk
        } else {
            standIn.calculateEntityAnimation(MorphFlight.flies(standIn.getType()));
        }
        animateEating(standIn, player);

        if (jumped) onJump(standIn, id);
        if (RESTING.contains(id) && (jumped || moved(player))) stopResting(standIn, id);
        if (standIn instanceof Camel camel && DASHING.containsKey(id)) camel.setDashing(true); // its tick ends a dash on landing
        countDown(FLAPPING, id, () -> {});
        tickTilt(id);
        countDown(STANDING, id, () -> {
            if (standIn instanceof PolarBear bear) bear.setStanding(false);
        });
        countDown(DASHING, id, () -> {
            if (standIn instanceof Camel camel) camel.setDashing(false);
        });
        tickTimed(id);
        if (standIn instanceof Strider strider) strider.setSuffocating(!FormTraits.isWarmStrider(player)); // shivers off lava
        countDown(REARING, id, () -> {
            if (standIn instanceof AbstractHorse horse) horse.setStanding(false);
        });
        countDown(RAMMING, id, () -> standIn.handleEntityEvent((byte) 59)); // goat raises its head
        countDown(EATING, id, () -> {
            if (standIn instanceof AbstractHorse horse) horse.setEating(false);
        });
    }

    /** Ticks a timer down and runs {@code done} when it reaches zero. */
    private static void countDown(Map<UUID, Integer> timers, UUID id, Runnable done) {
        Integer left = timers.get(id);
        if (left == null) return;
        if (left <= 0) {
            timers.remove(id);
            done.run();
        } else {
            timers.put(id, left - 1);
        }
    }

    /** A form's action, shown with the mob's own animation (its entity events where it has them). */
    static void perform(LivingEntity standIn, UUID player, FormAction action) {
        LAST_ACTION.put(player, action);
        switch (action) {
            case RAM -> {
                standIn.handleEntityEvent((byte) 58); // goat lowers its head
                RAMMING.put(player, RAM_TICKS);
            }
            case SWING -> standIn.handleEntityEvent((byte) 4); // iron golem swings
            case GRAZE -> {
                if (standIn instanceof Sheep) standIn.handleEntityEvent((byte) 10);
                if (standIn instanceof AbstractHorse horse) horse.setEating(true);
                EATING.put(player, EAT_TICKS);
            }
            case ROLL_UP -> { // as the mob's AI does: roll into the ball, then stay curled
                if (!(standIn instanceof Armadillo armadillo)) return;
                CURLED_YAW.put(player, armadillo.yBodyRot);
                armadillo.switchToState(Armadillo.ArmadilloState.ROLLING);
                startTimed(player, ROLL_UP_TICKS, () -> {
                    armadillo.switchToState(Armadillo.ArmadilloState.SCARED);
                    CURLED.add(player);
                });
            }
            case UNROLL -> { // and unroll before standing up again
                if (!(standIn instanceof Armadillo armadillo)) return;
                armadillo.switchToState(Armadillo.ArmadilloState.UNROLLING);
                CURLED.remove(player);
                startTimed(player, UNROLL_TICKS, () -> {
                    armadillo.switchToState(Armadillo.ArmadilloState.IDLE);
                    CURLED_YAW.remove(player);
                });
            }
            case STAND -> {
                if (standIn instanceof PolarBear bear) bear.setStanding(true);
                STANDING.put(player, STAND_TICKS);
            }
            case SWING_ARM -> standIn.swing(InteractionHand.MAIN_HAND);
            case DASH -> {
                if (standIn instanceof Camel camel) camel.setDashing(true);
                DASHING.put(player, DASH_TICKS);
            }
            case SIT -> {
                        if (standIn instanceof TamableAnimal pet) pet.setInSittingPose(true);
                if (standIn instanceof Camel camel) camel.sitDown();
                RESTING.add(player);
            }
            case HANG -> {
                if (standIn instanceof Bat bat) bat.setResting(true);
                RESTING.add(player);
            }
            case PLAY_DEAD, WAKE -> {
                if (standIn instanceof Axolotl axolotl) axolotl.setPlayingDead(action == FormAction.PLAY_DEAD);
            }
            case PUFF, DEFLATE -> {
                if (standIn instanceof Pufferfish puffer) puffer.setPuffState(action == FormAction.PUFF ? 2 : 0);
            }
            case CROAK -> holdPose(standIn, player, Pose.CROAKING, CROAK_TICKS);
            case TONGUE -> holdPose(standIn, player, Pose.USING_TONGUE, TONGUE_TICKS);
            case FLIP -> TILTS.put(player, new Tilt(FLIP_TICKS, FLIP_TICKS, FLIP_DEGREES, true));
            case LUNGE -> TILTS.put(player, new Tilt(LUNGE_TICKS, LUNGE_TICKS, LUNGE_DEGREES, false));
            case FLAP -> FLAPPING.put(player, FLAP_TICKS);
            case DIG -> {
                if (!(standIn instanceof Sniffer sniffer)) return;
                sniffer.transitionTo(Sniffer.State.DIGGING);
                startTimed(player, DIG_TICKS, () -> sniffer.transitionTo(Sniffer.State.IDLING));
            }
        }
    }

    /** {@code Frog}: its croak and tongue animations start when its pose changes. */
    private static void holdPose(LivingEntity standIn, UUID player, Pose pose, int ticks) {
        if (!(standIn instanceof Frog)) return;
        standIn.setPose(pose);
        startTimed(player, ticks, () -> standIn.setPose(Pose.STANDING));
    }

    /** Starts a held pose, ending any other first. */
    private static void startTimed(UUID player, int ticks, Runnable end) {
        Timed previous = TIMED.put(player, new Timed(ticks, end));
        if (previous != null) previous.end().run();
    }

    private static void tickTimed(UUID player) {
        Timed timed = TIMED.get(player);
        if (timed == null) return;
        if (timed.left() <= 0) {
            TIMED.remove(player);
            timed.end().run();
        } else {
            TIMED.put(player, new Timed(timed.left() - 1, timed.end()));
        }
    }

    private static boolean moved(Player player) {
        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;
        return dx * dx + dy * dy + dz * dz > STILL_DISTANCE_SQR;
    }

    private static void stopResting(LivingEntity standIn, UUID player) {
        RESTING.remove(player);
        if (standIn instanceof TamableAnimal pet) pet.setInSittingPose(false);
        if (standIn instanceof Camel camel) camel.standUp();
        if (standIn instanceof Bat bat) bat.setResting(false);
    }

    private record Timed(int left, Runnable end) {}

    /** A tilt of {@code degrees} at its peak, {@code left} of {@code total} ticks to go, about the body's middle or its feet. */
    private record Tilt(int left, int total, float degrees, boolean aboutMiddle) {}

    private static void tickTilt(UUID player) {
        Tilt tilt = TILTS.get(player);
        if (tilt == null) return;
        if (tilt.left() <= 0) TILTS.remove(player);
        else TILTS.put(player, new Tilt(tilt.left() - 1, tilt.total(), tilt.degrees(), tilt.aboutMiddle()));
    }

    /** How far the body is tipped forward right now (negative: nose up), rising and falling over the tilt. */
    static float tiltDegrees(UUID player, float partialTick) {
        Tilt tilt = TILTS.get(player);
        if (tilt == null) return 0.0F;
        float progress = Math.min(1.0F, (tilt.total() - tilt.left() + partialTick) / tilt.total());
        return tilt.degrees() * (float) Math.sin(Math.PI * progress);
    }

    /** Whether the current tilt turns about the body's middle (a bee's flip) rather than its feet. */
    static boolean tiltsAboutMiddle(UUID player) {
        Tilt tilt = TILTS.get(player);
        return tilt != null && tilt.aboutMiddle();
    }

    /** Whether an armadillo is all the way curled up, so moving would unroll it. */
    static boolean isCurled(UUID player) {
        return CURLED.contains(player);
    }

    /** The body yaw a curled armadillo holds, or null when it isn't curled. */
    static Float curledYaw(UUID player) {
        return CURLED_YAW.get(player);
    }

    /** Whether the stand-in should hang its head to eat (mobs with no eat pose of their own, like the cow). */
    static boolean headDown(UUID player) {
        return EATING.containsKey(player);
    }

    /**
     * Eating food shows the mob's own eat animation: a sheep's graze, a horse's lowered head; any other
     * form hangs its head while it eats.
     */
    private static void animateEating(LivingEntity standIn, Player player) {
        boolean eating = player.isUsingItem() && player.getUseItem().getUseAnimation() == UseAnim.EAT;
        if (!eating) return;
        UUID id = player.getUUID();
        if (standIn instanceof Sheep && !EATING.containsKey(id)) standIn.handleEntityEvent((byte) 10);
        if (standIn instanceof AbstractHorse horse) horse.setEating(true);
        EATING.put(id, Math.max(EATING.getOrDefault(id, 0), 2));
    }

    /** Test seam: the last action shown for the player, then forgotten. */
    static FormAction takeLastAction(UUID player) {
        return LAST_ACTION.remove(player);
    }

    /** Stand-in classes whose own tick threw, so they're drawn without animation. */
    static Set<Class<?>> failures() {
        return Set.copyOf(FAILED);
    }

    static void forget(UUID player) {
        WAS_ON_GROUND.remove(player);
        REARING.remove(player);
        RAMMING.remove(player);
        EATING.remove(player);
        DASHING.remove(player);
        STANDING.remove(player);
        FLAPPING.remove(player);
        TILTS.remove(player);
        LAST_ACTION.remove(player);
        CURLED_YAW.remove(player);
        CURLED.remove(player);
        TIMED.remove(player);
        RESTING.remove(player);
    }

    private static void onJump(LivingEntity standIn, UUID player) {
        if (standIn instanceof Rabbit) standIn.handleEntityEvent((byte) 1); // the hop
        if (standIn instanceof AbstractHorse horse) {
            horse.setStanding(true);
            REARING.put(player, HORSE_REAR_TICKS);
        }
    }

    private static void follow(LivingEntity standIn, Player player) {
        standIn.setPos(player.getX(), player.getY(), player.getZ());
        standIn.setDeltaMovement(Vec3.ZERO);
        standIn.setOnGround(player.onGround());
        standIn.setSprinting(player.isSprinting());
    }

    /**
     * A mob whose tick throws once is left frozen rather than crashing the client every frame. The tick's
     * own walk-cycle update is held back (it would count the player's movement a second time and run the
     * legs at double speed); {@link #tick} updates it once afterwards.
     */
    private static void runOwnTick(LivingEntity standIn) {
        if (FAILED.contains(standIn.getClass())) return;
        standIn.tickCount++; // ClientLevel#tickNonPassenger ages a mob before its tick; models animate on its age
        double xo = standIn.xo;
        double yo = standIn.yo;
        double zo = standIn.zo;
        float walkSpeed = standIn.walkAnimation.speed();
        standIn.xo = standIn.getX(); // no movement and no speed: its own update adds nothing
        standIn.yo = standIn.getY();
        standIn.zo = standIn.getZ();
        standIn.walkAnimation.setSpeed(0.0F);
        try {
            standIn.tick();
        } catch (RuntimeException e) {
            FAILED.add(standIn.getClass());
            BalancedTotems.LOGGER.warn("Morph form {} can't animate; drawing it still", standIn.getType(), e);
        } finally {
            standIn.xo = xo;
            standIn.yo = yo;
            standIn.zo = zo;
            standIn.walkAnimation.setSpeed(walkSpeed);
        }
    }
}
