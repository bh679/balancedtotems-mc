package games.brennan.balancedtotems.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: a Balanced Totem popped on {@code entityId}. Stands in for vanilla's entity
 * event 35, which always shows the vanilla totem in the pop animation.
 */
public record TotemPopPayload(int entityId, ItemStack totem) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeItem(totem);
    }

    public static TotemPopPayload decode(FriendlyByteBuf buf) {
        return new TotemPopPayload(buf.readVarInt(), buf.readItem());
    }
}
