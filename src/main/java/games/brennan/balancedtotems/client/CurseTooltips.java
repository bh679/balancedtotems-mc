package games.brennan.balancedtotems.client;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.curse.Tallies;
import games.brennan.balancedtotems.effect.HeartbreakEffect;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hover text for curses in the inventory's effect list: hearts Heartbreak takes, how to cure Zombified,
 * and what Rage or Lonely still want. Forge 1.20.1 has no effect-tooltip event, so this draws the tooltip
 * itself: on the wide list, which has none, just the curse lines; on the narrow (icon-only) list, vanilla's
 * name and time with the curse lines under them, drawn over vanilla's own.
 */
@Mod.EventBusSubscriber(modid = BalancedTotems.MOD_ID, value = Dist.CLIENT)
public final class CurseTooltips {

    /** {@code EffectRenderingInventoryScreen#renderEffects} layout. */
    private static final int WIDE_PANEL = 120;
    private static final int NARROW_PANEL = 32;
    private static final int ROW_HEIGHT = 33;
    private static final int ROWS_BEFORE_SQUEEZE = 5;
    private static final int SQUEEZED_HEIGHT = 132;

    private CurseTooltips() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof EffectRenderingInventoryScreen<?> screen)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        int left = screen.getGuiLeft() + screen.getXSize() + 2;
        boolean wide = screen.width - left >= WIDE_PANEL;
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        if (mouseX < left || mouseX > left + (wide ? WIDE_PANEL : NARROW_PANEL)) return;

        List<MobEffectInstance> effects = player.getActiveEffects().stream()
            .filter(effect -> IClientMobEffectExtensions.of(effect).isVisibleInInventory(effect)).sorted().toList();
        int rowHeight = effects.size() > ROWS_BEFORE_SQUEEZE ? SQUEEZED_HEIGHT / (effects.size() - 1) : ROW_HEIGHT;
        int top = screen.getGuiTop();
        MobEffectInstance hovered = null;
        for (MobEffectInstance effect : effects) {
            if (mouseY >= top && mouseY <= top + rowHeight) hovered = effect;
            top += rowHeight;
        }
        if (hovered == null) return;
        List<Component> lines = lines(hovered, player);
        if (lines.isEmpty()) return;
        if (!wide) {
            List<Component> withVanilla = new ArrayList<>();
            withVanilla.add(Component.translatable(hovered.getDescriptionId()));
            withVanilla.add(MobEffectUtil.formatDuration(hovered, 1.0F));
            withVanilla.addAll(lines);
            lines = withVanilla;
        }
        event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
    }

    /** The extra lines for a curse, or none for any other effect. */
    static List<Component> lines(MobEffectInstance effect, Player player) {
        List<Component> lines = new ArrayList<>();
        Component left = MobEffectUtil.formatDuration(effect, 1.0F);
        if (effect.getEffect() == BtRegistries.HEARTBREAK.get()) {
            lines.add(Component.translatable("effect.balancedtotems.heartbreak.hearts", heartsTaken(effect, player)).withStyle(ChatFormatting.GRAY));
        } else if (effect.getEffect() == BtRegistries.ZOMBIFIED.get()) {
            lines.add(Component.translatable("effect.balancedtotems.zombified.cure").withStyle(ChatFormatting.GRAY));
        } else if (effect.getEffect() == BtRegistries.RAMPAGE.get()) {
            int kills = Math.max(0, Tallies.needed(effect) - Tallies.count(player, Curse.RAMPAGE));
            lines.add(Component.translatable("effect.balancedtotems.rampage.deadline", kills, left).withStyle(ChatFormatting.GRAY));
        } else if (effect.getEffect() == BtRegistries.VOW_OF_PEACE.get() || effect.getEffect() == BtRegistries.MELON_DIET.get() || effect.getEffect() == BtRegistries.RESTRAINED.get()) {
            String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).getPath();
            lines.add(Component.translatable("effect.balancedtotems." + id + ".rule").withStyle(ChatFormatting.GRAY));
        } else if (effect.getEffect() == BtRegistries.LONELY.get()) {
            int friends = Math.max(0, Tallies.needed(effect) - Tallies.count(player, Curse.LONELINESS));
            lines.add(Component.translatable("effect.balancedtotems.lonely.deadline", friends, left).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /** Hearts Heartbreak takes right now (its last level may take less), as "2" or "9.5". */
    private static String heartsTaken(MobEffectInstance effect, Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = maxHealth == null ? null : maxHealth.getModifier(HeartbreakEffect.MODIFIER_ID);
        double health = modifier != null ? -modifier.getAmount() : HeartbreakEffect.HEALTH_PER_LEVEL * (effect.getAmplifier() + 1);
        double hearts = health / 2.0;
        return hearts == Math.floor(hearts) ? Integer.toString((int) hearts) : Double.toString(hearts);
    }
}
