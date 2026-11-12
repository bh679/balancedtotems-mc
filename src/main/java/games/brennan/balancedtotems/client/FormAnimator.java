package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.morph.FormAction;
import games.brennan.balancedtotems.morph.MorphFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
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

    private static final Map<UUID, Boolean> WAS_ON_GROUND = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REARING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RAMMING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EATING = new ConcurrentHashMap<>();
    private static final Set<Class<?>> FAILED = ConcurrentHashMap.newKeySet();

    private FormAnimator() {}

    static void tick(LivingEntity standIn, Player player) {
        UUID id = player.getUUID();
        boolean jumped = WAS_ON_GROUND.getOrDefault(id, true) && !player.onGround() && player.getDeltaMovement().y > 0;
        WAS_ON_GROUND.put(id, player.onGround());

        standIn.setOldPosAndRot();
        follow(standIn, player);
        runOwnTick(standIn);
        follow(standIn, player);
        standIn.calculateEntityAnimation(MorphFlight.flies(standIn.getType()));

        if (jumped) onJump(standIn, id);
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
        switch (action) {
            case RAM -> {
                standIn.handleEntityEvent((byte) 58); // goat lowers its head
                RAMMING.put(player, RAM_TICKS);
            }
            case SWING -> standIn.handleEntityEvent((byte) 4); // iron golem swings
            case GRAZE -> {
                if (standIn instanceof Sheep) standIn.handleEntityEvent((byte) 10);
                if (standIn instanceof AbstractHorse horse) {
                    horse.setEating(true);
                    EATING.put(player, EAT_TICKS);
                }
            }
        }
    }

    static void forget(UUID player) {
        WAS_ON_GROUND.remove(player);
        REARING.remove(player);
        RAMMING.remove(player);
        EATING.remove(player);
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

    /** A mob whose tick throws once is left frozen rather than crashing the client every frame. */
    private static void runOwnTick(LivingEntity standIn) {
        if (FAILED.contains(standIn.getClass())) return;
        try {
            standIn.tick();
        } catch (RuntimeException e) {
            FAILED.add(standIn.getClass());
            BalancedTotems.LOGGER.warn("Morph form {} can't animate; drawing it still", standIn.getType(), e);
        }
    }
}
