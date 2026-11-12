package games.brennan.dungeontotems;

import games.brennan.dungeontotems.curse.Curses;
import games.brennan.dungeontotems.net.TotemPopPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.EffectCures;
import games.brennan.dungeontotems.net.TotemNetwork;

/**
 * Vanilla's totem pop ({@code LivingEntity#checkTotemDeathProtection}) with our item in the
 * animation, then the totem's curse on top.
 */
public final class TotemPop {

    private TotemPop() {}

    /** Uses one totem from {@code held}, saves {@code player} at 1 HP and applies the curse. */
    public static void pop(ServerPlayer player, ItemStack held, Curse curse) {
        ItemStack used = held.copyWithCount(1);
        held.shrink(1);

        player.awardStat(Stats.ITEM_USED.get(used.getItem()), 1);
        CriteriaTriggers.USED_TOTEM.trigger(player, used);
        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);

        player.setHealth(1.0F);
        player.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        // A single-purpose totem (void, fall, explosion) gives no Fire Resistance; the Fire Totem does.
        if (curse.givesFireResistance()) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        applyCurse(player, curse);

        TotemNetwork.sendToTrackingAndSelf(player, new TotemPopPayload(player.getId(), used));
    }

    /**
     * You broke a held rule totem's rule: that totem takes your life and gives it back, popping just as
     * if you had died, and chat says why.
     */
    public static void popFromRule(ServerPlayer player, ItemStack held, Curse curse) {
        pop(player, held, curse);
        player.displayClientMessage(Component.translatable("message.dungeontotems." + curse.itemName() + ".broken")
            .withStyle(ChatFormatting.RED), false);
    }

    /** Earlier curses survive the clear above (curses carry no totem cure), so this one stacks on them. */
    private static void applyCurse(ServerPlayer player, Curse curse) {
        if (curse.effect() != null) Curses.pop(player, curse);
    }
}
