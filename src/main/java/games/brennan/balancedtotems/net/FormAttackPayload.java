package games.brennan.balancedtotems.net;

import games.brennan.balancedtotems.BalancedTotems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a morphed player with a ranged attack (llama spit, snowball) attacked the air. */
public record FormAttackPayload() implements CustomPacketPayload {

    public static final FormAttackPayload INSTANCE = new FormAttackPayload();
    public static final Type<FormAttackPayload> TYPE = new Type<>(BalancedTotems.id("form_attack"));
    public static final StreamCodec<ByteBuf, FormAttackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
