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

    /** "While held" (held totems) then "After it saves you", or what a narrow totem saves you from. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.dungeontotems." + curse.itemName();
        if (curse.isNarrow()) {
            tooltip.add(Component.translatable(key + ".curse").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        if (curse.isHeld()) tooltip.add(Component.translatable(key + ".held").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(key + ".saved").withStyle(ChatFormatting.GOLD));
    }
}
