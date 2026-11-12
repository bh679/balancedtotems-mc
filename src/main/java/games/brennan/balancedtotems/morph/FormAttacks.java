package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.curse.DeadlineCurses;
import games.brennan.balancedtotems.net.BtNetwork;
import games.brennan.balancedtotems.net.MorphActionPayload;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A form attacks the way its mob does. Goats ram, iron golems throw the target up, polar bears rear,
 * llamas spit and snow golems throw snowballs (at a mob, or at range by attacking the air),
 * pufferfish poison, frogs swallow small slimes and magma cubes, bees
 * sting once (poison) and then may die like a stung bee unless the morph ends first, and forms with no
 * attack do no damage. Sheep, cows and horses graze instead: hitting grass eats it.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class FormAttacks {

    private static final double RAM_KNOCKBACK = 2.5;
    /** Speed of a goat's charge into the ram, and the little hop it gives. */
    private static final double RAM_CHARGE = 0.9;
    private static final double RAM_HOP = 0.15;
    private static final double GOLEM_TOSS = 0.4;
    private static final int GRAZE_COOLDOWN_TICKS = 40;
    private static final int STING_DEATH_ODDS_CAP = 1200;
    /** {@code Pufferfish#touch} at full puff. */
    private static final int PUFFERFISH_POISON_TICKS = 120;
    /** Biome temperatures that make cold and warm frogs. */
    private static final float COLD_FROG = 0.2F;
    private static final float WARM_FROG = 1.0F;

    /** {@code Llama}'s ranged attack goal: one spit every two seconds. */
    private static final int SPIT_COOLDOWN = 40;
    /** Mobs with an attack attribute that never attack: allays, parrots, and rabbits other than the killer bunny. */
    private static final Set<EntityType<?>> PEACEFUL = Set.of(EntityType.ALLAY, EntityType.PARROT, EntityType.RABBIT);
    /** Forms whose attack is a ranged shot; the client sends attacks on the air for these. */
    public static final Set<EntityType<?>> RANGED = Set.of(EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.SNOW_GOLEM);

    private static final Set<EntityType<?>> GRAZERS = Set.of(EntityType.SHEEP, EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
        EntityType.COW, EntityType.MOOSHROOM);

    /** Ticks since each stung bee-player stung; the stinger is gone, and so, soon, is the bee. */
    private static final Map<UUID, Integer> STUNG = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_GRAZE = new ConcurrentHashMap<>();

    private FormAttacks() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null) return;
        Entity target = event.getTarget();
        if (form == EntityType.GOAT) {
            ram(player, target);
        } else if (form == EntityType.IRON_GOLEM) {
            swing(player, target);
        } else if (form == EntityType.BEE) {
            if (STUNG.containsKey(player.getUUID())) event.setCanceled(true); // no stinger left
            else sting(player, target);
        } else if (form == EntityType.PUFFERFISH) {
            event.setCanceled(true);
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, PUFFERFISH_POISON_TICKS, 0), player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PUFFER_FISH_STING, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } else if (RANGED.contains(form)) {
            event.setCanceled(true); // the shot does the damage
            rangedAttack(player);
        } else if (form == EntityType.FROG) {
            if (tongue(player, target)) event.setCanceled(true);
        } else if (!hasAttack(form)) {
            event.setCanceled(true);
        } else if (form == EntityType.POLAR_BEAR) {
            show(player, FormAction.STAND);
        } else {
            show(player, FormAction.SWING_ARM); // a zombified piglin's or zombie's arm swing
        }
    }

    /**
     * {@code ShootTongue}: a frog's tongue swallows a small slime or magma cube whole (a magma cube
     * leaves a froglight); anything else just takes the hit. True if it swallowed the target.
     */
    private static boolean tongue(ServerPlayer player, Entity target) {
        show(player, FormAction.TONGUE);
        if (!(target instanceof Slime slime) || slime.getSize() != 1) return false;
        player.level().playSound(null, player.blockPosition(), SoundEvents.FROG_EAT, SoundSource.PLAYERS, 2.0F, 1.0F);
        if (slime instanceof MagmaCube) {
            player.level().addFreshEntity(new ItemEntity(player.level(), slime.getX(), slime.getY(), slime.getZ(), new ItemStack(froglight(player))));
        }
        slime.remove(Entity.RemovalReason.KILLED);
        return true;
    }

    /** The froglight a frog from this climate makes: cold verdant, warm pearlescent, else ochre. */
    private static Item froglight(ServerPlayer player) {
        float temperature = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
        if (temperature < COLD_FROG) return Items.VERDANT_FROGLIGHT;
        if (temperature >= WARM_FROG) return Items.PEARLESCENT_FROGLIGHT;
        return Items.OCHRE_FROGLIGHT;
    }

    /** Whether the mob has a melee attack of its own. */
    public static boolean hasAttack(EntityType<?> form) {
        return !PEACEFUL.contains(form) && !RANGED.contains(form) && DefaultAttributes.hasSupplier(form)
            && DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) form).hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    /** A goat charges into the ram: you sprint forward at the target as it is knocked away. */
    private static void ram(ServerPlayer player, Entity target) {
        Vec3 charge = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
        if (charge.lengthSqr() > 1.0E-4) {
            player.setSprinting(true);
            player.setDeltaMovement(charge.normalize().scale(RAM_CHARGE).add(0.0, RAM_HOP, 0.0));
            player.connection.send(new ClientboundSetEntityMotionPacket(player)); // the client moves the player
        }
        if (target instanceof LivingEntity living) {
            living.knockback(RAM_KNOCKBACK, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GOAT_RAM_IMPACT, SoundSource.PLAYERS, 1.0F, 1.0F);
        show(player, FormAction.RAM);
    }

    private static void swing(ServerPlayer player, Entity target) {
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, GOLEM_TOSS, 0.0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        show(player, FormAction.SWING);
    }

    /** {@code Bee#doHurtTarget}: poison by difficulty, then the stinger is gone. */
    private static void sting(ServerPlayer player, Entity target) {
        Difficulty difficulty = player.level().getDifficulty();
        int seconds = difficulty == Difficulty.NORMAL ? 10 : difficulty == Difficulty.HARD ? 18 : 0;
        if (seconds > 0 && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0), player);
        }
        STUNG.put(player.getUUID(), 0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** A llama spits and a snow golem throws a snowball where you look; other forms have no ranged attack. */
    public static void rangedAttack(ServerPlayer player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == EntityType.SNOW_GOLEM) LandMoves.throwSnowball(player);
        else if (form == EntityType.LLAMA || form == EntityType.TRADER_LLAMA) spit(player);
    }

    /** {@code Llama#spit}, aimed where you look. */
    private static void spit(ServerPlayer player) {
        if (!FormTimers.ready(player, "spit", SPIT_COOLDOWN)) return;
        LlamaSpit spit = new LlamaSpit(EntityType.LLAMA_SPIT, player.level());
        spit.setOwner(player);
        spit.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3 look = player.getLookAngle();
        spit.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
        player.level().addFreshEntity(spit);
        player.level().playSound(null, player.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Hitting grass as a sheep, cow or horse eats it: the block turns to dirt, the tuft is gone. */
    @SubscribeEvent
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null || !GRAZERS.contains(form)) return;
        Level level = player.level();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean block = state.is(Blocks.GRASS_BLOCK);
        boolean tuft = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS);
        if (!block && !tuft) return;
        event.setCanceled(true);
        long now = level.getGameTime();
        if (now - LAST_GRAZE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < GRAZE_COOLDOWN_TICKS) return;
        LAST_GRAZE.put(player.getUUID(), now);
        level.levelEvent(2001, pos, Block.getId(state)); // break particles and sound, as a sheep's
        level.setBlockAndUpdate(pos, block ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState());
        player.getFoodData().eat(1, 0.1F);
        show(player, FormAction.GRAZE);
    }

    /** {@code Bee#customServerAiStep}: a bee without its stinger dies at a random moment, likelier as time passes. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer since = STUNG.get(player.getUUID());
        if (since == null) return;
        int ticks = since + 1;
        STUNG.put(player.getUUID(), ticks);
        if (ticks % 5 == 0 && player.getRandom().nextInt(Mth.clamp(STING_DEATH_ODDS_CAP - ticks, 1, STING_DEATH_ODDS_CAP)) == 0) {
            STUNG.remove(player.getUUID());
            DeadlineCurses.doom(player, BtRegistries.LOST_STINGER);
        }
    }

    /** From {@link Morphs#setForm}: a new form means a new stinger, so turning back in time saves you. */
    static void onFormChanged(ServerPlayer player, @Nullable EntityType<?> form) {
        if (form != EntityType.BEE) STUNG.remove(player.getUUID());
    }

    /** Test seam: whether the player stung as a bee and still might die of it. */
    public static boolean hasStung(ServerPlayer player) {
        return STUNG.containsKey(player.getUUID());
    }

    private static void show(ServerPlayer player, FormAction action) {
        BtNetwork.sendToTrackingAndSelf(player, new MorphActionPayload(player.getId(), action.ordinal()));
    }
}
