package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

/**
 * Gifts for the Lonely curse, each restarting its timer ({@link CurseTimers#gift}): a trade with a
 * villager, food you threw that a villager picked up, or a gift to a PlayerMob
 * ({@link games.brennan.dungeontotems.compat.PlayerMobGifts}). Villagers always count: giving harms nobody.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class LonelyGifts {

    /** How close a villager must be to a vanished food item to have picked it up. */
    private static final double PICKUP_REACH = 1.5;

    private LonelyGifts() {}

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAbstractVillager() instanceof Villager) {
            CurseTimers.gift(player);
        }
    }

    /** Food a lonely player threw, gone because a villager next to it picked it up. */
    @SubscribeEvent
    public static void onItemGone(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item) || item.getRemovalReason() != Entity.RemovalReason.DISCARDED) return;
        if (!(item.getOwner() instanceof ServerPlayer player) || !player.hasEffect(TotemRegistries.LONELY)) return;
        if (!Villager.FOOD_POINTS.containsKey(item.getItem().getItem())) return;
        if (!event.getLevel().getEntitiesOfClass(Player.class, item.getBoundingBox().inflate(PICKUP_REACH)).isEmpty()) return;
        if (!event.getLevel().getEntitiesOfClass(Villager.class, item.getBoundingBox().inflate(PICKUP_REACH)).isEmpty()) {
            CurseTimers.gift(player);
        }
    }
}
