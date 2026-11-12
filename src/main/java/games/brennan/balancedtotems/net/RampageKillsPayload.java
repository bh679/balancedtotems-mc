package games.brennan.balancedtotems.net;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: your Rampage kills so far, for the effect's hover text. */
public record RampageKillsPayload(int kills) implements CustomPacketPayload {

    public static final Type<RampageKillsPayload> TYPE = new Type<>(BalancedTotems.id("rampage_kills"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RampageKillsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RampageKillsPayload::kills,
        RampageKillsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
