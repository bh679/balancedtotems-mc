package games.brennan.balancedtotems;

import games.brennan.balancedtotems.curse.Curses;
import games.brennan.balancedtotems.net.TotemPopPayload;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.EffectCures;
import games.brennan.balancedtotems.net.BtNetwork;

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
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        applyCurse(player, curse);

        BtNetwork.sendToTrackingAndSelf(player, new TotemPopPayload(player.getId(), used));
    }

    /**
     * Earlier curses survive the clear above (curses carry no totem cure), so the same curse is
     * still here when it pops again: then it goes up a level with a fresh timer.
     */
    private static void applyCurse(ServerPlayer player, Curse curse) {
        Holder<MobEffect> effect = curse.effect();
        if (effect == null) return;
        MobEffectInstance current = player.getEffect(effect);
        int amplifier = current == null ? curse.baseAmplifier() : current.getAmplifier() + 1;
        Curses.apply(player, curse, amplifier);
    }
}
