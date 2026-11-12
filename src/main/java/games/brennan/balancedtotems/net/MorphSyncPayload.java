package games.brennan.balancedtotems.net;

import net.minecraft.network.FriendlyByteBuf;

/** Server to client: player {@code entityId} now looks like {@code form} (an entity type id), or themself when empty. */
public record MorphSyncPayload(int entityId, String form) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(form);
    }

    public static MorphSyncPayload decode(FriendlyByteBuf buf) {
        return new MorphSyncPayload(buf.readVarInt(), buf.readUtf());
    }
}
