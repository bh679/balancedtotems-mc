package games.brennan.balancedtotems.net;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: a Balanced Totem popped on {@code entityId}. Stands in for vanilla's entity
 * event 35, which always shows the vanilla totem in the pop animation.
 */
public record TotemPopPayload(int entityId, ItemStack totem) implements CustomPacketPayload {

    public static final Type<TotemPopPayload> TYPE = new Type<>(BalancedTotems.id("totem_pop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TotemPopPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, TotemPopPayload::entityId,
        ItemStack.STREAM_CODEC, TotemPopPayload::totem,
        TotemPopPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
