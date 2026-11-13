package games.brennan.dungeontotems.morph;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.net.MorphSyncPayload;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import games.brennan.dungeontotems.net.TotemNetwork;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToIntFunction;

/**
 * The morph engine shared by the Morphing and Zombified totems: a player's form is an entity type
 * kept in {@link TotemRegistries#FORM}. It drives their stats (here), hitbox and mob reactions
 * ({@link MorphEvents}) and how everyone sees them (client {@code MorphRenderer}).
 */
public final class Morphs {

    private static final double AGGRO_DROP_RADIUS = 32.0;

    private static final ResourceLocation MAX_HEALTH_ID = DungeonTotems.id("morph.max_health");
    private static final ResourceLocation SPEED_ID = DungeonTotems.id("morph.speed");
    private static final ResourceLocation ATTACK_ID = DungeonTotems.id("morph.attack_damage");
    private static final ResourceLocation JUMP_ID = DungeonTotems.id("morph.jump");
    private static final ResourceLocation STEP_ID = DungeonTotems.id("morph.step_height");
    private static final ResourceLocation FORM_JUMP_ID = DungeonTotems.id("morph.form_jump");
    /**
     * Jump strengths a form's default attributes don't capture: horses always make their best jump (vanilla
     * rolls 0.4 to 1.0 per horse), camels can't jump (they step up blocks), frogs leap.
     */
    private static final Map<EntityType<?>, Double> FORM_JUMP = Map.of(
        EntityType.HORSE, 1.0, EntityType.SKELETON_HORSE, 1.0, EntityType.ZOMBIE_HORSE, 1.0,
        EntityType.CAMEL, 0.0, EntityType.FROG, 0.7);

    private Morphs() {}

    /**
     * Turns the player into a random form from the pool for where they are (in lava, underwater, or
     * anywhere else; an empty lava or water pool falls back to land), weighted by the
     * {@link MorphForm#DATA_MAP}, among the forms that fit where they stand.
     */
    public static void startRandomMorph(ServerPlayer player) {
        MorphForm.Pool pool = poolFor(player);
        List<EntityType<?>> fitting = fitting(player, pool);
        if (fitting.isEmpty() && pool != MorphForm.Pool.LAND) fitting = fitting(player, MorphForm.Pool.LAND);
        if (fitting.isEmpty()) {
            DungeonTotems.LOGGER.debug("No morph fits around {}; staying a player", player.getScoreboardName());
            return;
        }
        setForm(player, pick(fitting, Morphs::weightOf, player.getRandom()));
    }

    static MorphForm.Pool poolFor(Player player) {
        if (player.isInLava()) return MorphForm.Pool.LAVA;
        if (player.isEyeInFluid(FluidTags.WATER)) return MorphForm.Pool.WATER;
        return MorphForm.Pool.LAND;
    }

    /** The pool's forms with weight above 0 that fit around the player. */
    private static List<EntityType<?>> fitting(ServerPlayer player, MorphForm.Pool pool) {
        List<EntityType<?>> fitting = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getDataMap(MorphForm.DATA_MAP).forEach((key, form) -> {
            if (form.pool() != pool || form.weight() <= 0) return;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
            if (type != null && DefaultAttributes.hasSupplier(type) && fits(player, type)) fitting.add(type);
        });
        return fitting;
    }

    /** The form's pool and weight, if it has one. */
    public static Optional<MorphForm> formData(EntityType<?> type) {
        return Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).getData(MorphForm.DATA_MAP));
    }

    private static int weightOf(EntityType<?> type) {
        return formData(type).map(MorphForm::weight).orElse(0);
    }

    /** A weighted random pick; every item needs a weight above 0. */
    static <T> T pick(List<T> items, ToIntFunction<T> weight, RandomSource random) {
        int total = items.stream().mapToInt(weight).sum();
        int roll = random.nextInt(total);
        for (T item : items) {
            roll -= weight.applyAsInt(item);
            if (roll < 0) return item;
        }
        return items.get(items.size() - 1);
    }

    /** Called when the Morphed or Zombified curse ends: falls back to whichever is still running. */
    public static void onCurseEnded(ServerPlayer player, Holder<MobEffect> ending) {
        boolean zombified = !ending.is(TotemRegistries.ZOMBIFIED) && player.hasEffect(TotemRegistries.ZOMBIFIED);
        boolean morphed = !ending.is(TotemRegistries.MORPHED) && player.hasEffect(TotemRegistries.MORPHED);
        if (zombified) setForm(player, EntityType.ZOMBIE);
        else if (morphed) startRandomMorph(player);
        else setForm(player, null);
    }

    /** Sets (or with null, clears) the player's form, then re-applies stats, hitbox and sync. */
    public static void setForm(ServerPlayer player, @Nullable EntityType<?> type) {
        EntityType<?> previous = formOf(player).orElse(null);
        String id = type == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        player.setData(TotemRegistries.FORM, id);
        applyStats(player, type);
        player.refreshDimensions();
        if (type != null) dropMonsterAggro(player);
        FormTimers.onFormChanged(player);
        MorphFlight.onFormChanged(player, previous, type);
        FormAttacks.onFormChanged(player, type);
        FormTraits.onFormChanged(player, type);
        FormUse.onFormChanged(player, type);
        WaterLife.onFormChanged(player);
        TotemNetwork.sendToTrackingAndSelf(player, new MorphSyncPayload(player.getId(), id));
    }

    /** The player's current form, if any. Safe on both sides and during entity construction. */
    public static Optional<EntityType<?>> formOf(Player player) {
        if (!player.hasData(TotemRegistries.FORM)) return Optional.empty();
        String id = player.getData(TotemRegistries.FORM);
        if (id.isEmpty()) return Optional.empty();
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
    }

    /** Re-applies stat modifiers for the stored form (they are transient, so a relog needs this). */
    @SuppressWarnings("unchecked")
    static void applyStats(Player player, @Nullable EntityType<?> type) {
        clear(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID);
        clear(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        clear(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
        clear(player, Attributes.JUMP_STRENGTH, JUMP_ID);
        clear(player, Attributes.STEP_HEIGHT, STEP_ID);
        clear(player, Attributes.JUMP_STRENGTH, FORM_JUMP_ID);
        if (type == null || !DefaultAttributes.hasSupplier(type)) return;

        AttributeSupplier mob = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        add(player, mob, Attributes.MAX_HEALTH, MAX_HEALTH_ID, MorphStats::maxHealthDelta, AttributeModifier.Operation.ADD_VALUE);
        MobGaits.Gait gait = MobGaits.of(type);
        if (WaterLife.isLandlocked(type)) { // can't walk on land (swimming doesn't use this speed)
            add(player, mob, Attributes.MOVEMENT_SPEED, SPEED_ID, mobSpeed -> -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else if (gait == null) { // flyers
            add(player, mob, Attributes.MOVEMENT_SPEED, SPEED_ID, MorphStats::speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else { // land forms walk at their mob's stroll
            add(player, mob, Attributes.MOVEMENT_SPEED, SPEED_ID,
                mobSpeed -> MobGaits.walkSpeed(gait, mobSpeed) / MobGaits.PLAYER_WALK_SPEED - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        add(player, mob, Attributes.ATTACK_DAMAGE, ATTACK_ID, MorphStats::attackDelta, AttributeModifier.Operation.ADD_VALUE);
        if (!FORM_JUMP.containsKey(type)) {
            add(player, mob, Attributes.JUMP_STRENGTH, JUMP_ID, MorphStats::jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        add(player, mob, Attributes.STEP_HEIGHT, STEP_ID, MorphStats::stepDelta, AttributeModifier.Operation.ADD_VALUE);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null && FORM_JUMP.containsKey(type)) { // to exactly this strength, from the player's own
            jump.addTransientModifier(new AttributeModifier(FORM_JUMP_ID, FORM_JUMP.get(type) - jump.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void add(Player player, AttributeSupplier mob, Holder<Attribute> attribute, ResourceLocation id,
                            DoubleUnaryOperator toAmount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || !mob.hasAttribute(attribute)) return;
        double amount = toAmount.applyAsDouble(mob.getBaseValue(attribute));
        if (amount == 0.0) return;
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void clear(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static boolean fits(ServerPlayer player, EntityType<?> type) {
        AABB box = type.getDimensions().makeBoundingBox(player.position());
        return player.level().noCollision(player, box);
    }

    /** Monsters already chasing the player lose interest; {@link MorphEvents} stops new ones. */
    private static void dropMonsterAggro(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(AGGRO_DROP_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, m -> m.getTarget() == player)) {
            if (MorphEvents.ignoresMorphed(mob, player)) mob.setTarget(null);
        }
    }
}
