package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Friends for the Lonely curse. Each friend counts once. With PlayerMob installed a friend is a PlayerMob
 * you swapped gifts with ({@link games.brennan.balancedtotems.compat.PlayerMobGifts}); when villagers stand
 * in ({@link Targets}), a villager you trade with, feed, or cure from zombification.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class LonelyFriends {

    /** How close a villager must be to a vanished food item to have picked it up. */
    private static final double PICKUP_REACH = 1.5;

    private LonelyFriends() {}

    /** {@code friend} is now the player's friend; counts toward Lonely if it is a new one. */
    public static void befriend(ServerPlayer player, UUID friend) {
        if (!player.hasEffect(BtRegistries.LONELY)) return;
        List<String> known = player.getData(BtRegistries.LONELY_FRIEND_IDS);
        String id = friend.toString();
        if (known.contains(id)) return;
        List<String> updated = new ArrayList<>(known);
        updated.add(id);
        player.setData(BtRegistries.LONELY_FRIEND_IDS, List.copyOf(updated));
        Entity entity = player.level().getEntity(friend);
        Component who = entity != null ? entity.getDisplayName() : Component.translatable("message.balancedtotems.lonely.someone");
        Tallies.add(player, Curse.LONELINESS, who);
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAbstractVillager() instanceof Villager villager
            && Targets.villagersStandIn()) {
            befriend(player, villager.getUUID());
        }
    }

    /** Food a lonely player threw, gone because a villager next to it picked it up. */
    @SubscribeEvent
    public static void onItemGone(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item) || item.getRemovalReason() != Entity.RemovalReason.DISCARDED) return;
        if (!(item.getOwner() instanceof ServerPlayer player) || !player.hasEffect(BtRegistries.LONELY)) return;
        if (!item.getItem().has(DataComponents.VILLAGER_FOOD) || !Targets.villagersStandIn()) return;
        if (!event.getLevel().getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(PICKUP_REACH)).isEmpty()) return;
        event.getLevel().getEntitiesOfClass(Villager.class, item.getBoundingBox().inflate(PICKUP_REACH)).stream()
            .findFirst().ifPresent(villager -> befriend(player, villager.getUUID()));
    }

    /** A cured zombie villager remembers who cured it (a major positive gossip). */
    @SubscribeEvent
    public static void onCured(LivingConversionEvent.Post event) {
        if (!(event.getOutcome() instanceof Villager villager) || !Targets.villagersStandIn()) return;
        for (ServerPlayer player : villager.level().getServer().getPlayerList().getPlayers()) {
            if (villager.getGossips().getReputation(player.getUUID(), type -> type == GossipType.MAJOR_POSITIVE) > 0) {
                befriend(player, villager.getUUID());
            }
        }
    }
}
