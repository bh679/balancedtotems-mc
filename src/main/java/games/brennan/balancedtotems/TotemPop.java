package games.brennan.balancedtotems;

import games.brennan.balancedtotems.morph.Morphs;
import games.brennan.balancedtotems.net.TotemPopPayload;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.network.PacketDistributor;

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

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new TotemPopPayload(player.getId(), used));
    }

    private static void applyCurse(ServerPlayer player, Curse curse) {
        Holder<MobEffect> effect = curse.effect();
        if (effect == null) return;
        if (curse == Curse.RAMPAGE) player.setData(BtRegistries.RAMPAGE_KILLS, 0);
        // showIcon true, visible particles off: a curse is a status, not a potion cloud.
        player.addEffect(new MobEffectInstance(effect, curse.durationTicks(), 0, false, false, true));
        if (curse == Curse.MORPHING) Morphs.startRandomMorph(player);
        if (curse == Curse.ZOMBIFIED) Morphs.setForm(player, EntityType.ZOMBIE);
    }
}
