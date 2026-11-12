package games.brennan.balancedtotems.net;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: player {@code entityId} now looks like {@code form} (an entity type id), or themself when empty. */
public record MorphSyncPayload(int entityId, String form) implements CustomPacketPayload {

    public static final Type<MorphSyncPayload> TYPE = new Type<>(BalancedTotems.id("morph_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MorphSyncPayload::entityId,
        ByteBufCodecs.STRING_UTF8, MorphSyncPayload::form,
        MorphSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
