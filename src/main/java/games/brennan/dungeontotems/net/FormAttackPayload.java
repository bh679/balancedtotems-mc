package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a morphed player attacked the air (a shot, a goat's ram, or a noise). */
public record FormAttackPayload() implements CustomPacketPayload {

    public static final FormAttackPayload INSTANCE = new FormAttackPayload();
    public static final Type<FormAttackPayload> TYPE = new Type<>(DungeonTotems.id("form_attack"));
    public static final StreamCodec<ByteBuf, FormAttackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
