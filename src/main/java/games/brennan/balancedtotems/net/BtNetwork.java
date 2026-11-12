package games.brennan.balancedtotems.net;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/** The mod's one channel: server-to-client packets for totem pops, forms and curse counts. */
public final class BtNetwork {

    private static final String PROTOCOL_VERSION = "2";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        BalancedTotems.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private BtNetwork() {}

    /** Called from the {@link BalancedTotems} constructor. */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, TotemPopPayload.class, TotemPopPayload::encode, TotemPopPayload::decode,
            (payload, context) -> {
                context.get().enqueueWork(() -> games.brennan.balancedtotems.client.ClientPayloads.onTotemPop(payload));
                context.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, MorphSyncPayload.class, MorphSyncPayload::encode, MorphSyncPayload::decode,
            (payload, context) -> {
                context.get().enqueueWork(() -> games.brennan.balancedtotems.client.ClientPayloads.onMorphSync(payload));
                context.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id, CurseCountPayload.class, CurseCountPayload::encode, CurseCountPayload::decode,
            (payload, context) -> {
                context.get().enqueueWork(() -> games.brennan.balancedtotems.client.ClientPayloads.onCurseCount(payload));
                context.get().setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Sends to {@code player} and everyone who can see them, skipping any connection without our
     * channel (a vanilla client, or a GameTest mock player).
     */
    public static void sendToTrackingAndSelf(ServerPlayer player, Object payload) {
        sendTo(player, payload);
        for (ServerPlayer watcher : player.serverLevel().getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (watcher != player) sendTo(watcher, payload);
        }
    }

    public static void sendTo(ServerPlayer player, Object payload) {
        Connection connection = player.connection == null ? null : player.connection.connection;
        // A GameTest mock player's connection has no netty channel at all.
        if (connection != null && connection.channel() != null && CHANNEL.isRemotePresent(connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}
