package games.brennan.dungeontotems.net;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: morphed player {@code entityId} did {@code action} (a {@code FormAction} ordinal), for its animation. */
public record MorphActionPayload(int entityId, int action) implements CustomPacketPayload {

    public static final Type<MorphActionPayload> TYPE = new Type<>(DungeonTotems.id("morph_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MorphActionPayload::entityId,
        ByteBufCodecs.VAR_INT, MorphActionPayload::action,
        MorphActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
