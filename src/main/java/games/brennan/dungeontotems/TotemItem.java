package games.brennan.dungeontotems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Dungeon Totem. It does nothing on use: {@link TotemTriggerEvents} fires it from your hand. */
public class TotemItem extends Item {

    private final Curse curse;

    public TotemItem(Curse curse, Properties properties) {
        super(properties);
        this.curse = curse;
    }

    public Curse curse() {
        return curse;
    }

    /**
     * "While held" (held totems) then "After it saves you", or what a narrow totem saves you from. A
     * stackable totem adds the levels this stack gives and how far it stacks.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.dungeontotems." + curse.itemName();
        if (curse.isNarrow()) {
            tooltip.add(Component.translatable(key + ".curse").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        if (curse.isHeld()) tooltip.add(Component.translatable(key + ".held").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(key + ".saved").withStyle(ChatFormatting.GOLD));
        if (curse.isStackable()) {
            tooltip.add(Component.translatable("item.dungeontotems.stack_level",
                level(curse.heldAmplifier(stack.getCount())), level(curse.afterSaveAmplifier(stack.getCount())))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("item.dungeontotems.stacks", curse.maxStack()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** "I", "II", "III": vanilla's level numerals. */
    private static Component level(int amplifier) {
        return Component.translatable("enchantment.level." + (amplifier + 1));
    }
}
