package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.curse.Tallies;
import games.brennan.balancedtotems.effect.HeartbreakEffect;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Hover text for curses in the inventory's effect list: hearts Heartbreak takes, how to cure Zombified,
 * and what Rage or Lonely still want. On 26.x NeoForge asks for these on both the narrow and the wide effect list.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class CurseTooltips {

    private CurseTooltips() {}

    @SubscribeEvent
    public static void onEffectTooltip(GatherEffectScreenTooltipsEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) event.getTooltip().addAll(lines(event.getEffectInstance(), player));
    }


    /** The extra lines for a curse, or none for any other effect. */
    static List<Component> lines(MobEffectInstance effect, Player player) {
        List<Component> lines = new ArrayList<>();
        Component left = MobEffectUtil.formatDuration(effect, 1.0F, player.level().tickRateManager().tickrate());
        if (effect.is(BtRegistries.HEARTBREAK)) {
            lines.add(Component.translatable("effect.balancedtotems.heartbreak.hearts", heartsTaken(effect, player)).withStyle(ChatFormatting.GRAY));
        } else if (effect.is(BtRegistries.ZOMBIFIED)) {
            lines.add(Component.translatable("effect.balancedtotems.zombified.cure").withStyle(ChatFormatting.GRAY));
        } else if (effect.is(BtRegistries.RAMPAGE)) {
            int kills = Math.max(0, Tallies.needed(effect) - Tallies.count(player, Curse.RAMPAGE));
            lines.add(Component.translatable("effect.balancedtotems.rampage.deadline", kills, left).withStyle(ChatFormatting.GRAY));
        } else if (effect.is(BtRegistries.VOW_OF_PEACE) || effect.is(BtRegistries.MELON_DIET) || effect.is(BtRegistries.RESTRAINED)) {
            String id = effect.getEffect().unwrapKey().map(key -> key.identifier().getPath()).orElse("");
            lines.add(Component.translatable("effect.balancedtotems." + id + ".rule").withStyle(ChatFormatting.GRAY));
        } else if (effect.is(BtRegistries.LONELY)) {
            int friends = Math.max(0, Tallies.needed(effect) - Tallies.count(player, Curse.LONELINESS));
            lines.add(Component.translatable("effect.balancedtotems.lonely.deadline", friends, left).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /** Hearts Heartbreak takes right now (its last level may take less), as "2" or "9.5". */
    private static String heartsTaken(MobEffectInstance effect, Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = maxHealth == null ? null : maxHealth.getModifier(HeartbreakEffect.MODIFIER_ID);
        double health = modifier != null ? -modifier.amount() : HeartbreakEffect.HEALTH_PER_LEVEL * (effect.getAmplifier() + 1);
        double hearts = health / 2.0;
        return hearts == Math.floor(hearts) ? Integer.toString((int) hearts) : Double.toString(hearts);
    }
}
