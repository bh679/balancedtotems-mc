package games.brennan.balancedtotems.voidtotem;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.api.BalancedTotemsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Void Totem's chorus-fruit hop. Goes to the {@link BalancedTotemsApi} rescue target (the
 * train, under Dungeon Train), else the last safe ground the player stood on, else world spawn.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class VoidRescue {

    private static final int TRACK_EVERY_TICKS = 20;

    private record SafeSpot(ResourceKey<Level> dimension, Vec3 pos) {}

    private static final Map<UUID, SafeSpot> LAST_SAFE = new ConcurrentHashMap<>();

    private VoidRescue() {}

    public static void rescue(ServerPlayer player) {
        Vec3 from = player.position();
        ServerLevel level = player.serverLevel();
        ServerLevel toLevel = level;
        Vec3 to = BalancedTotemsApi.rescueTarget(player).orElse(null);

        if (to == null) {
            SafeSpot spot = LAST_SAFE.get(player.getUUID());
            ServerLevel spotLevel = spot == null ? null : player.server.getLevel(spot.dimension());
            if (spotLevel != null) {
                toLevel = spotLevel;
                to = spot.pos();
            } else {
                BlockPos spawn = level.getSharedSpawnPos();
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
                to = Vec3.atBottomCenterOf(top);
            }
        }

        level.playSound(null, from.x, from.y, from.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.stopRiding();
        player.teleportTo(toLevel, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        toLevel.broadcastEntityEvent(player, (byte) 46); // portal particles, as a chorus fruit hop
        toLevel.playSound(null, to.x, to.y, to.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Remembers where the player last stood on solid ground, once a second. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % TRACK_EVERY_TICKS != 0) return;
        if (!player.onGround() || player.isPassenger() || player.isInLiquid()) return;
        BlockPos below = player.blockPosition().below();
        if (!player.level().getBlockState(below).isFaceSturdy(player.level(), below, Direction.UP)) return;
        LAST_SAFE.put(player.getUUID(), new SafeSpot(player.level().dimension(), player.position()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SAFE.remove(event.getEntity().getUUID());
    }
}
