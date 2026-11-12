package games.brennan.dungeontotems.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;

/**
 * What other mods may plug into Dungeon Totems.
 *
 * <p>Dungeon Train sets the {@link RescueTarget} so the Void Totem lands you back on the train.
 * Changing a signature here is a MAJOR version bump.</p>
 */
public final class DungeonTotemsApi {

    /** Where a Void Totem should put a player, in the player's current dimension. */
    @FunctionalInterface
    public interface RescueTarget {
        Optional<Vec3> find(ServerPlayer player);
    }

    private static volatile RescueTarget rescueTarget = player -> Optional.empty();

    private DungeonTotemsApi() {}

    /** Replaces the rescue target. With none set, the Void Totem uses the player's last safe ground. */
    public static void setRescueTarget(RescueTarget target) {
        rescueTarget = Objects.requireNonNull(target, "target");
    }

    public static Optional<Vec3> rescueTarget(ServerPlayer player) {
        try {
            return rescueTarget.find(player);
        } catch (RuntimeException e) {
            games.brennan.dungeontotems.DungeonTotems.LOGGER.error("Void Totem rescue target failed", e);
            return Optional.empty();
        }
    }
}
