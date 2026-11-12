package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.TotemPop;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
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

/**
 * Two rule curses. Break the rule holding the totem and that totem pops; after it has saved you, you
 * drop to half a heart on your next tick ({@link SavedCurseShield}).
 * <ul>
 *   <li><b>Melon Diet</b>: eat anything but a melon slice (cake included). Drinks are fine.</li>
 *   <li><b>Restrained</b>: open a chest, trapped chest, barrel or ender chest. Like Dungeon Train's
 *       "Respect The Rules", sneaking with a block in hand is placing, not opening.</li>
 * </ul>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class RuleCurses {

    private RuleCurses() {}

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (breaksMelonDiet(event.getItem(), player)) broke(player, Curse.MELON, TotemRegistries.BROKEN_DIET);
    }

    static boolean breaksMelonDiet(ItemStack stack, ServerPlayer player) {
        if (stack.is(Items.MELON_SLICE) || stack.getUseAnimation() != UseAnim.EAT) return false;
        return stack.getFoodProperties(player) != null;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (isCake(block) && player.canEat(false)) {
            broke(player, Curse.MELON, TotemRegistries.BROKEN_DIET);
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
