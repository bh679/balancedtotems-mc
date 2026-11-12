package games.brennan.dungeontotems.morph;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.net.MorphSyncPayload;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

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

    private Morphs() {}

    /** Turns the player into a random mob from the morph pool that fits where they stand. */
    public static void startRandomMorph(ServerPlayer player) {
        List<EntityType<?>> fitting = new ArrayList<>();
        for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(TotemRegistries.MORPH_POOL)) {
            EntityType<?> type = holder.value();
            if (DefaultAttributes.hasSupplier(type) && fits(player, type)) fitting.add(type);
        }
        if (fitting.isEmpty()) {
            DungeonTotems.LOGGER.debug("No morph fits around {}; staying a player", player.getScoreboardName());
            return;
        }
        RandomSource random = player.getRandom();
        setForm(player, fitting.get(random.nextInt(fitting.size())));
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
        String id = type == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        player.setData(TotemRegistries.FORM, id);
        applyStats(player, type);
        player.refreshDimensions();
        if (type != null) dropMonsterAggro(player);
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
        if (type == null || !DefaultAttributes.hasSupplier(type)) return;

        AttributeSupplier mob = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        add(player, mob, Attributes.MAX_HEALTH, MAX_HEALTH_ID, MorphStats::maxHealthDelta, AttributeModifier.Operation.ADD_VALUE);
        add(player, mob, Attributes.MOVEMENT_SPEED, SPEED_ID, MorphStats::speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(player, mob, Attributes.ATTACK_DAMAGE, ATTACK_ID, MorphStats::attackDelta, AttributeModifier.Operation.ADD_VALUE);
        add(player, mob, Attributes.JUMP_STRENGTH, JUMP_ID, MorphStats::jumpBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(player, mob, Attributes.STEP_HEIGHT, STEP_ID, MorphStats::stepDelta, AttributeModifier.Operation.ADD_VALUE);
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
