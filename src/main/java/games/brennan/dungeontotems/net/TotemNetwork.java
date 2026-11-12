package games.brennan.dungeontotems.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every payload. Handlers reach the client code only inside the lambda body, so a
 * dedicated server never loads a client class.
 */
public final class TotemNetwork {

    private static final String PROTOCOL_VERSION = "3";

    private TotemNetwork() {}

    /** Mod-bus listener, added in {@link games.brennan.dungeontotems.DungeonTotems}. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(TotemPopPayload.TYPE, TotemPopPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> games.brennan.dungeontotems.client.ClientPayloads.onTotemPop(payload)));
        registrar.playToClient(MorphSyncPayload.TYPE, MorphSyncPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> games.brennan.dungeontotems.client.ClientPayloads.onMorphSync(payload)));
        registrar.playToClient(CurseTimerPayload.TYPE, CurseTimerPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> games.brennan.dungeontotems.client.ClientPayloads.onCurseTimer(payload)));
    }

    /**
     * Sends to {@code player} and everyone who can see them, skipping any connection without our
     * channel (a vanilla client, or a GameTest mock player): NeoForge throws on those.
     */
    public static void sendToTrackingAndSelf(ServerPlayer player, CustomPacketPayload payload) {
        sendTo(player, payload);
        for (ServerPlayer watcher : player.serverLevel().getChunkSource().chunkMap.getPlayersWatching(player)) {
            if (watcher != player) sendTo(watcher, payload);
        }
    }

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
