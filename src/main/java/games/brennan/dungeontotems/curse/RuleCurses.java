package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.TotemPop;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two rule curses. Break the rule holding the totem and that totem pops; after it has saved you, you
 * drop to half a heart on your next tick ({@link SavedCurseShield}).
 * <ul>
 *   <li><b>Melon Diet</b>: eat anything but a melon slice (cake included). Drinks are fine. Food that
 *       breaks the diet doesn't fill you, and cake isn't eaten at all.</li>
 *   <li><b>Restrained</b>: open a chest, trapped chest, barrel or ender chest. Like Dungeon Train's
 *       "Respect The Rules", sneaking with a block in hand is placing, not opening.</li>
 * </ul>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class RuleCurses {

    /** Hunger and saturation when a dieting player started eating something that isn't melon. */
    private record Fullness(int food, float saturation) {}
    private static final Map<UUID, Fullness> BEFORE_EATING = new ConcurrentHashMap<>();

    private RuleCurses() {}

    /** Remembers how full a dieting player was, so food that breaks the diet can't fill them. */
    @SubscribeEvent
    public static void onStartEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !dieting(player) || !breaksMelonDiet(event.getItem(), player)) return;
        FoodData food = player.getFoodData();
        BEFORE_EATING.put(player.getUUID(), new Fullness(food.getFoodLevel(), food.getSaturationLevel()));
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Fullness before = BEFORE_EATING.remove(player.getUUID());
        if (!breaksMelonDiet(event.getItem(), player) || !dieting(player)) return;
        if (before != null) {
            player.getFoodData().setFoodLevel(before.food());
            player.getFoodData().setSaturation(before.saturation());
        }
        broke(player, Curse.MELON_DIET, TotemRegistries.BROKEN_DIET);
    }

    /** Under the melon diet: holding its totem, or cursed with it. */
    static boolean dieting(ServerPlayer player) {
        return HeldCurses.heldTotem(player, Curse.MELON_DIET) != null || player.hasEffect(TotemRegistries.MELON_DIET);
    }

    static boolean breaksMelonDiet(ItemStack stack, ServerPlayer player) {
        if (stack.is(Items.MELON_SLICE) || stack.getUseAnimation() != UseAnim.EAT) return false;
        return stack.getFoodProperties(player) != null;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (isCake(block) && player.canEat(false) && dieting(player)) {
            // Cake isn't melon: no slice for you, and the diet is broken all the same.
            event.setCanceled(true);
            broke(player, Curse.MELON_DIET, TotemRegistries.BROKEN_DIET);
        }
        if (isLootContainer(block)
            && !(player.isSecondaryUseActive() && event.getItemStack().getItem() instanceof BlockItem)) {
            broke(player, Curse.RESTRAINT, TotemRegistries.BROKEN_RESTRAINT);
        }
    }

    /**
     * {@code player} broke {@code curse}'s rule: the held totem pops if there is one, otherwise the curse
     * dooms you (which after a save leaves you on half a heart).
     */
    static void broke(ServerPlayer player, Curse curse, ResourceKey<DamageType> cause) {
        ItemStack totem = HeldCurses.heldTotem(player, curse);
        if (totem != null) {
            TotemPop.popFromRule(player, totem, curse);
        } else if (player.hasEffect(curse.effect())) {
            DeadlineCurses.doom(player, cause);
        }
    }

    private static boolean isCake(Block block) {
        return block instanceof CakeBlock || block instanceof CandleCakeBlock;
    }

    private static boolean isLootContainer(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof EnderChestBlock;
    }
}
