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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Void Totem's rescue. With a {@link BalancedTotemsApi} rescue target (the train, under Dungeon
 * Train) it goes straight there. Otherwise it acts like a chorus fruit: from the last safe ground the
 * player stood on (else world spawn), a random hop onto ground within 8 blocks, as many tries as the
 * fruit takes; if none lands, the player stays on that safe ground.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class VoidRescue {

    private static final int TRACK_EVERY_TICKS = 20;
    /** {@code ChorusFruitItem}: 16 tries, 8 blocks each way. */
    private static final int HOP_TRIES = 16;
    private static final double HOP_RANGE = 16.0;
    private static final int HOP_HEIGHT = 16;

    private record SafeSpot(ResourceKey<Level> dimension, Vec3 pos) {}

    private static final Map<UUID, SafeSpot> LAST_SAFE = new ConcurrentHashMap<>();

    private VoidRescue() {}

    public static void rescue(ServerPlayer player) {
        Vec3 from = player.position();
        ServerLevel level = player.level();
        ServerLevel toLevel = level;
        Vec3 to = BalancedTotemsApi.rescueTarget(player).orElse(null);
        boolean hop = to == null;

        if (hop) {
            SafeSpot spot = LAST_SAFE.get(player.getUUID());
            ServerLevel spotLevel = spot == null ? null : player.level().getServer().getLevel(spot.dimension());
            if (spotLevel != null) {
                toLevel = spotLevel;
                to = spot.pos();
            } else {
                LevelData.RespawnData respawn = level.getRespawnData();
                ServerLevel spawnLevel = level.getServer().getLevel(respawn.dimension());
                if (spawnLevel != null) toLevel = spawnLevel;
                BlockPos spawn = respawn.pos();
                BlockPos top = toLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
                to = Vec3.atBottomCenterOf(top);
            }
        }

        level.playSound(null, from.x, from.y, from.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.stopRiding();
        player.teleportTo(toLevel, to.x, to.y, to.z, Set.of(), player.getYRot(), player.getXRot(), true);
        if (hop) hopNearby(player, toLevel);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        toLevel.broadcastEntityEvent(player, (byte) 46); // portal particles, as a chorus fruit hop
        toLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** A chorus fruit's random hop from where the player now stands; stays put if no try lands. */
    private static void hopNearby(ServerPlayer player, ServerLevel level) {
        int floor = level.getMinY();
        int ceiling = floor + level.getLogicalHeight() - 1;
        for (int i = 0; i < HOP_TRIES; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * HOP_RANGE;
            double y = Mth.clamp(player.getY() + (player.getRandom().nextInt(HOP_HEIGHT) - HOP_HEIGHT / 2), floor, ceiling);
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * HOP_RANGE;
            if (player.randomTeleport(x, y, z, false, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) return;
        }
    }

    /** Test seam: as if the player had last stood safely at {@code pos}. */
    public static void rememberSafeSpot(ServerPlayer player, Vec3 pos) {
        LAST_SAFE.put(player.getUUID(), new SafeSpot(player.level().dimension(), pos));
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
