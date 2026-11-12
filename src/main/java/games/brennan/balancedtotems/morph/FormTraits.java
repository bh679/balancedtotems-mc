package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a form is like to live in, beyond its stats and flight:
 * <ul>
 *   <li>Chickens fall slowly; water forms swim fast; turtles breathe underwater; lava forms and
 *       zombified piglins shrug off fire and lava. Each is a
 *       hidden, endless effect that lasts exactly as long as the form.</li>
 *   <li>Water forms breathe in water and dry out on land like the mob: fish, squid and tadpoles
 *       in 15 seconds, dolphins in 2 minutes, axolotls in 5; after that they take damage.</li>
 *   <li>Snow golems leave snow where they walk and melt in warm biomes, rain and water.</li>
 *   <li>Falls hurt as they hurt the mob: cats, chickens, golems and flyers take none (the vanilla
 *       {@code fall_damage_immune} tag), frogs and goats fall further first, horses take half.</li>
 *   <li>Land forms walk at their mob's stroll and sprint at its run: a cow's panic, a horse's ridden
 *       gallop ({@link MobGaits}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class FormTraits {

    private static final int FISH_DRY_TICKS = 300;
    private static final int DOLPHIN_DRY_TICKS = 2400;
    private static final int AXOLOTL_DRY_TICKS = 6000;
    private static final int DRY_DAMAGE_EVERY_TICKS = 20;
    private static final float DRY_DAMAGE = 2.0F;

    /** Every hidden effect a form can bring (see {@link #effectsFor}). */
    private static final List<Holder<MobEffect>> FORM_EFFECTS =
        List.of(MobEffects.SLOW_FALLING, MobEffects.DOLPHINS_GRACE, MobEffects.FIRE_RESISTANCE, MobEffects.WATER_BREATHING);

    private static final Map<UUID, Integer> DRY_TICKS = new ConcurrentHashMap<>();

    /** {@code Frog#calculateFallDamage} and {@code Goat#calculateFallDamage}: blocks fallen before it hurts. */
    private static final float FROG_SAFE_FALL = 5.0F;
    private static final float GOAT_SAFE_FALL = 10.0F;
    private static final Set<EntityType<?>> HORSES = Set.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
        EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE, EntityType.CAMEL, EntityType.LLAMA, EntityType.TRADER_LLAMA);
    private static final ResourceLocation RUN_ID = BalancedTotems.id("morph.run");

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null) return;
        if (form.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) event.setCanceled(true);
        else if (form == EntityType.FROG) event.setDistance(Math.max(0.0F, event.getDistance() - FROG_SAFE_FALL));
        else if (form == EntityType.GOAT) event.setDistance(Math.max(0.0F, event.getDistance() - GOAT_SAFE_FALL));
        else if (HORSES.contains(form)) event.setDistance(event.getDistance() * 0.5F); // AbstractHorse halves it
    }

    /**
     * Sprinting as a land form runs at its mob's run ({@link MobGaits}): a cow's panic, a horse's ridden
     * gallop. The modifier takes the player's own +30% sprint into account; walking takes it away.
     */
    @SuppressWarnings("unchecked")
    private static void run(ServerPlayer player, @Nullable EntityType<?> form) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        MobGaits.Gait gait = MobGaits.of(form);
        double wanted = 0.0;
        if (player.isSprinting() && gait != null && DefaultAttributes.hasSupplier(form)) {
            double mobSpeed = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) form).getBaseValue(Attributes.MOVEMENT_SPEED);
            wanted = MobGaits.runSpeed(gait, mobSpeed) / (MobGaits.walkSpeed(gait, mobSpeed) * MobGaits.PLAYER_SPRINT) - 1.0;
        }
        AttributeModifier current = speed.getModifier(RUN_ID);
        if (current == null ? wanted == 0.0 : current.amount() == wanted) return;
        speed.removeModifier(RUN_ID);
        if (wanted != 0.0) speed.addTransientModifier(new AttributeModifier(RUN_ID, wanted, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** {@code Strider#SUFFOCATING_MODIFIER}: a strider off lava moves at two thirds of its speed. */
    public static final double STRIDER_COLD = 0.66;
    private static final ResourceLocation STRIDER_COLD_ID = BalancedTotems.id("morph.strider_cold");

    private FormTraits() {}

    /** Whether the player stands on this fluid: a strider on lava, unless sneaking to sink. */
    public static boolean standsOn(Player player, FluidState fluid) {
        return fluid.is(FluidTags.LAVA) && !player.isShiftKeyDown() && Morphs.formOf(player).orElse(null) == EntityType.STRIDER;
    }

    /** {@code Strider#tick}: warm on or in lava (or another strider-warm block), cold, and shivering, anywhere else. */
    public static boolean isWarmStrider(Player player) {
        return player.level().getBlockState(player.blockPosition()).is(BlockTags.STRIDER_WARM_BLOCKS)
            || player.level().getBlockState(player.blockPosition().below()).is(BlockTags.STRIDER_WARM_BLOCKS)
            || player.getFluidHeight(FluidTags.LAVA) > 0.0;
    }

    /**
     * A cold strider is slowed as the mob is. Ground speed grows with the square of a mob's speed
     * ({@link MobGaits#likeAMob}), so the player's speed takes the slowdown squared.
     */
    private static void chill(ServerPlayer player, @Nullable EntityType<?> form) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean cold = form == EntityType.STRIDER && !isWarmStrider(player);
        if (cold == speed.hasModifier(STRIDER_COLD_ID)) return;
        if (cold) speed.addTransientModifier(new AttributeModifier(STRIDER_COLD_ID, STRIDER_COLD * STRIDER_COLD - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else speed.removeModifier(STRIDER_COLD_ID);
    }

    /** From {@link Morphs#setForm}: swap the hidden effects to the new form's. */
    static void onFormChanged(ServerPlayer player, @Nullable EntityType<?> form) {
        DRY_TICKS.remove(player.getUUID());
        Set<Holder<MobEffect>> wantedEffects = effectsFor(form);
        for (Holder<MobEffect> effect : FORM_EFFECTS) {
            boolean wanted = wantedEffects.contains(effect);
            MobEffectInstance current = player.getEffect(effect);
            if (wanted && current == null) {
                player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
            } else if (!wanted && current != null && current.isInfiniteDuration() && current.isAmbient()) {
                player.removeEffect(effect);
            }
        }
    }

    private static Set<Holder<MobEffect>> effectsFor(@Nullable EntityType<?> form) {
        if (form == null) return Set.of();
        if (form == EntityType.CHICKEN) return Set.of(MobEffects.SLOW_FALLING);
        if (form == EntityType.TURTLE) return Set.of(MobEffects.WATER_BREATHING, MobEffects.DOLPHINS_GRACE);
        if (isLavaForm(form) || form.fireImmune()) return Set.of(MobEffects.FIRE_RESISTANCE);
        if (isWaterForm(form)) return Set.of(MobEffects.DOLPHINS_GRACE);
        return Set.of();
    }

    private static boolean isLavaForm(EntityType<?> form) {
        return Morphs.formData(form).filter(data -> data.pool() == MorphForm.Pool.LAVA).isPresent();
    }

    public static boolean isWaterForm(@Nullable EntityType<?> form) {
        return form != null && Morphs.formData(form).filter(data -> data.pool() == MorphForm.Pool.WATER).isPresent();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        run(player, form);
        chill(player, form);
        if (isWaterForm(form)) breathe(player, form);
        if (form == EntityType.SNOW_GOLEM) beSnowGolem(player);
    }

    /** In water: full air. Out of it: air runs down over the form's time, then the form takes damage. */
    private static void breathe(ServerPlayer player, EntityType<?> form) {
        boolean moistAnywhere = form == EntityType.DOLPHIN || form == EntityType.AXOLOTL;
        boolean wet = moistAnywhere ? player.isInWaterRainOrBubble() : player.isInWater();
        if (wet) {
            DRY_TICKS.remove(player.getUUID());
            player.setAirSupply(player.getMaxAirSupply());
            return;
        }
        int limit = form == EntityType.DOLPHIN ? DOLPHIN_DRY_TICKS : form == EntityType.AXOLOTL ? AXOLOTL_DRY_TICKS : FISH_DRY_TICKS;
        int dry = DRY_TICKS.merge(player.getUUID(), 1, Integer::sum);
        player.setAirSupply(Mth.clamp(player.getMaxAirSupply() * (limit - dry) / limit, 0, player.getMaxAirSupply()));
        if (dry > limit && dry % DRY_DAMAGE_EVERY_TICKS == 0) {
            DamageSource source = moistAnywhere ? player.damageSources().dryOut() : player.damageSources().drown();
            player.hurt(source, DRY_DAMAGE);
        }
    }

    /** {@code SnowGolem#aiStep}: melt where it's warm or wet, and leave snow underfoot. */
    private static void beSnowGolem(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.getBiome(player.blockPosition()).is(BiomeTags.SNOW_GOLEM_MELTS)) player.hurt(player.damageSources().onFire(), 1.0F);
        if (player.isInWaterRainOrBubble()) player.hurt(player.damageSources().drown(), 1.0F);
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        BlockState snow = Blocks.SNOW.defaultBlockState();
        for (int i = 0; i < 4; i++) {
            int x = Mth.floor(player.getX() + (i % 2 * 2 - 1) * 0.25F);
            int z = Mth.floor(player.getZ() + (i / 2 % 2 * 2 - 1) * 0.25F);
            BlockPos pos = new BlockPos(x, Mth.floor(player.getY()), z);
            if (level.getBlockState(pos).isAir() && snow.canSurvive(level, pos)) level.setBlockAndUpdate(pos, snow);
        }
    }
}
