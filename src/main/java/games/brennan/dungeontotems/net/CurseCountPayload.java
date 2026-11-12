package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: your Rage kills or Lonely friends so far ({@code curse} is a Curse name), for the hover text. */
public record CurseCountPayload(String curse, int count) implements CustomPacketPayload {

    public static final Type<CurseCountPayload> TYPE = new Type<>(DungeonTotems.id("curse_count"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CurseCountPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, CurseCountPayload::curse,
        ByteBufCodecs.VAR_INT, CurseCountPayload::count,
        CurseCountPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
