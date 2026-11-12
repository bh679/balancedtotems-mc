package games.brennan.balancedtotems.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A curse effect. {@code curable == false} gives it no curative items at all, so milk leaves it.
 * Another totem's pop leaves every curse alone: see {@code curse/CurseMarks}.
 */
public class CurseEffect extends MobEffect {

    private final boolean curable;

    public CurseEffect(MobEffectCategory category, int color, boolean curable) {
        super(category, color);
        this.curable = curable;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return curable ? super.getCurativeItems() : new ArrayList<>();
    }
}
