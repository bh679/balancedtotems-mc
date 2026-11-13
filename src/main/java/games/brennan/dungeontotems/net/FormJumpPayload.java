package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a morphed player whose form jumps its own way (a camel dashes) pressed jump. */
public record FormJumpPayload() implements CustomPacketPayload {

    public static final FormJumpPayload INSTANCE = new FormJumpPayload();
    public static final Type<FormJumpPayload> TYPE = new Type<>(DungeonTotems.id("form_jump"));
    public static final StreamCodec<ByteBuf, FormJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
