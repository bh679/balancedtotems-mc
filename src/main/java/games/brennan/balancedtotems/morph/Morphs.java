package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.net.MorphSyncPayload;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
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
import net.minecraftforge.common.ForgeMod;
import games.brennan.balancedtotems.net.BtNetwork;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;

/**
 * The morph engine shared by the Morphing and Zombified totems: a player's form is an entity type
 * kept in {@link BtRegistries#FORM}. It drives their stats (here), hitbox and mob reactions
 * ({@link MorphEvents}) and how everyone sees them (client {@code MorphRenderer}).
 */
public final class Morphs {

    private static final double AGGRO_DROP_RADIUS = 32.0;

    private static final UUID MAX_HEALTH_ID = UUID.fromString("0f6f9b52-3f0e-4d3b-9a77-2c1c7e5a9b01");
    private static final UUID SPEED_ID = UUID.fromString("0f6f9b52-3f0e-4d3b-9a77-2c1c7e5a9b02");
    private static final UUID ATTACK_ID = UUID.fromString("0f6f9b52-3f0e-4d3b-9a77-2c1c7e5a9b03");
    private static final UUID STEP_ID = UUID.fromString("0f6f9b52-3f0e-4d3b-9a77-2c1c7e5a9b05");
    private static final String MODIFIER_NAME = "balancedtotems:morph";

    private Morphs() {}

    /** Turns the player into a random mob from the morph pool that fits where they stand. */
    public static void startRandomMorph(ServerPlayer player) {
        List<EntityType<?>> fitting = new ArrayList<>();
        for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(BtRegistries.MORPH_POOL)) {
            EntityType<?> type = holder.value();
            if (DefaultAttributes.hasSupplier(type) && fits(player, type)) fitting.add(type);
        }
        if (fitting.isEmpty()) {
            BalancedTotems.LOGGER.debug("No morph fits around {}; staying a player", player.getScoreboardName());
            return;
        }
        RandomSource random = player.getRandom();
        setForm(player, fitting.get(random.nextInt(fitting.size())));
    }

    /** Called when the Morphed or Zombified curse ends: falls back to whichever is still running. */
    public static void onCurseEnded(ServerPlayer player, MobEffect ending) {
        boolean zombified = ending != BtRegistries.ZOMBIFIED.get() && player.hasEffect(BtRegistries.ZOMBIFIED.get());
        boolean morphed = ending != BtRegistries.MORPHED.get() && player.hasEffect(BtRegistries.MORPHED.get());
        if (zombified) setForm(player, EntityType.ZOMBIE);
        else if (morphed) startRandomMorph(player);
        else setForm(player, null);
    }

    /** Sets (or with null, clears) the player's form, then re-applies stats, hitbox and sync. */
    public static void setForm(ServerPlayer player, @Nullable EntityType<?> type) {
        String id = type == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        BtRegistries.FORM.set(player, id);
        applyStats(player, type);
        player.refreshDimensions();
        if (type != null) dropMonsterAggro(player);
        BtNetwork.sendToTrackingAndSelf(player, new MorphSyncPayload(player.getId(), id));
    }

    /** The player's current form, if any. Safe on both sides and during entity construction. */
    public static Optional<EntityType<?>> formOf(Player player) {
        if (!BtRegistries.FORM.has(player)) return Optional.empty();
        String id = BtRegistries.FORM.get(player);
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
        clear(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_ID);
        if (type == null || !DefaultAttributes.hasSupplier(type)) return;

        AttributeSupplier mob = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        add(player, mob, Attributes.MAX_HEALTH, MAX_HEALTH_ID, MorphStats::maxHealthDelta, AttributeModifier.Operation.ADDITION);
        add(player, mob, Attributes.MOVEMENT_SPEED, SPEED_ID, MorphStats::speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(player, mob, Attributes.ATTACK_DAMAGE, ATTACK_ID, MorphStats::attackDelta, AttributeModifier.Operation.ADDITION);
        // 1.20.1 has no player jump or step-height attribute: jump is scaled in MorphEvents#onJump, and
        // step height comes from Forge's step addition, read off the mob itself.
        double step = MorphStats.stepDelta(stepHeightOf(player, type));
        AttributeInstance stepAttribute = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepAttribute != null && step != 0.0) {
            stepAttribute.addTransientModifier(new AttributeModifier(STEP_ID, MODIFIER_NAME, step, AttributeModifier.Operation.ADDITION));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void add(Player player, AttributeSupplier mob, Attribute attribute, UUID id,
                            DoubleUnaryOperator toAmount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || !mob.hasAttribute(attribute)) return;
        double amount = toAmount.applyAsDouble(mob.getBaseValue(attribute));
        if (amount == 0.0) return;
        instance.addTransientModifier(new AttributeModifier(id, MODIFIER_NAME, amount, operation));
    }

    private static void clear(Player player, Attribute attribute, UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    /** A mob's step height: a throwaway copy of it, since 1.20.1 keeps step height on the entity. */
    private static double stepHeightOf(Player player, EntityType<?> type) {
        Entity mob = type.create(player.level());
        return mob == null ? MorphStats.PLAYER_STEP_HEIGHT : mob.getStepHeight();
    }

    /** Jump multiplier for the form: only mobs with a jump attribute (horses) jump differently. */
    public static double jumpMultiplier(Player player) {
        EntityType<?> type = formOf(player).orElse(null);
        if (type == null || !DefaultAttributes.hasSupplier(type)) return 1.0;
        @SuppressWarnings("unchecked")
        AttributeSupplier mob = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        if (!mob.hasAttribute(Attributes.JUMP_STRENGTH)) return 1.0;
        return 1.0 + MorphStats.jumpBonus(mob.getBaseValue(Attributes.JUMP_STRENGTH));
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
