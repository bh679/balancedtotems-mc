package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: seconds left on your Rage or Lonely timer ({@code curse} is a Curse name), for the hover text. */
public record CurseTimerPayload(String curse, int secondsLeft) implements CustomPacketPayload {

    public static final Type<CurseTimerPayload> TYPE = new Type<>(DungeonTotems.id("curse_timer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CurseTimerPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, CurseTimerPayload::curse,
        ByteBufCodecs.VAR_INT, CurseTimerPayload::secondsLeft,
        CurseTimerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
