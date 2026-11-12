package games.brennan.balancedtotems.net;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers both payloads. Handlers reach the client code only inside the lambda body, so a
 * dedicated server never loads a client class.
 */
public final class BtNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private BtNetwork() {}

    /** Mod-bus listener, added in {@link games.brennan.balancedtotems.BalancedTotems}. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(TotemPopPayload.TYPE, TotemPopPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> games.brennan.balancedtotems.client.ClientPayloads.onTotemPop(payload)));
        registrar.playToClient(MorphSyncPayload.TYPE, MorphSyncPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> games.brennan.balancedtotems.client.ClientPayloads.onMorphSync(payload)));
    }
}
