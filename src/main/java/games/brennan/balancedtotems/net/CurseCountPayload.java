package games.brennan.balancedtotems.net;

import net.minecraft.network.FriendlyByteBuf;

/** Server to client: your Rage kills or Lonely friends so far ({@code curse} is a Curse name), for the hover text. */
public record CurseCountPayload(String curse, int count) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(curse);
        buf.writeVarInt(count);
    }

    public static CurseCountPayload decode(FriendlyByteBuf buf) {
        return new CurseCountPayload(buf.readUtf(), buf.readVarInt());
    }
}
