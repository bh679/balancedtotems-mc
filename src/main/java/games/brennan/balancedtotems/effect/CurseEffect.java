package games.brennan.balancedtotems.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * A curse effect. {@code curable == false} gives it no cures at all: milk leaves it, and so does
 * another totem's pop (vanilla clears only effects cured by {@code PROTECTED_BY_TOTEM}).
 */
public class CurseEffect extends MobEffect {

    private final boolean curable;

    public CurseEffect(MobEffectCategory category, int color, boolean curable) {
        super(category, color);
        this.curable = curable;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        if (curable) super.fillEffectCures(cures, effectInstance);
    }
}
