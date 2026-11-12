package games.brennan.balancedtotems.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Two fewer hearts per level, but never below half a heart: the last level takes only what is left.
 * Milk cures it.
 */
public class HeartbreakEffect extends CurseEffect {

    public static final UUID MODIFIER_ID = UUID.fromString("6b3c1f0e-2a64-4c8e-9d4e-8f1b2a7c5d31");
    private static final String MODIFIER_NAME = "balancedtotems:effect.heartbreak";
    /** Max health taken per level: two hearts. */
    public static final double HEALTH_PER_LEVEL = 4.0;
    /** The least max health Heartbreak leaves you: half a heart. */
    public static final double HALF_HEART = 1.0;

    public HeartbreakEffect(int color) {
        super(MobEffectCategory.HARMFUL, color, true);
        // Registered so removal and the inventory's attribute lines know the modifier; the amount is set below.
        addAttributeModifier(Attributes.MAX_HEALTH, MODIFIER_ID.toString(), -HEALTH_PER_LEVEL, AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        AttributeInstance maxHealth = attributes.getInstance(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;
        maxHealth.removeModifier(MODIFIER_ID);
        double spare = Math.max(0.0, maxHealth.getValue() - HALF_HEART);
        double taken = Math.min(HEALTH_PER_LEVEL * (amplifier + 1), spare);
        maxHealth.addPermanentModifier(new AttributeModifier(MODIFIER_ID, MODIFIER_NAME, -taken, AttributeModifier.Operation.ADDITION));
    }

    /** True once Heartbreak has you down to half a heart, so another level would take nothing. */
    public static boolean atHalfHeart(LivingEntity entity) {
        return entity.getMaxHealth() <= HALF_HEART;
    }
}
