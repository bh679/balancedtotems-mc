package games.brennan.balancedtotems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Balanced Totem. It does nothing on use: {@link TotemTriggerEvents} fires it from your hand. */
public class TotemItem extends Item {

    private final Curse curse;

    public TotemItem(Curse curse, Properties properties) {
        super(properties);
        this.curse = curse;
    }

    public Curse curse() {
        return curse;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.balancedtotems." + curse.itemName() + ".curse")
            .withStyle(curse.onlySaves() != null ? ChatFormatting.DARK_AQUA : ChatFormatting.RED));
    }
}
