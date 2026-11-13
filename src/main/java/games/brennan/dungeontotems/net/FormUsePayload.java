package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a morphed player pressed "use" with an empty hand (a chicken lays, an armadillo rolls). */
public record FormUsePayload() implements CustomPacketPayload {

    public static final FormUsePayload INSTANCE = new FormUsePayload();
    public static final Type<FormUsePayload> TYPE = new Type<>(DungeonTotems.id("form_use"));
    public static final StreamCodec<ByteBuf, FormUsePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
