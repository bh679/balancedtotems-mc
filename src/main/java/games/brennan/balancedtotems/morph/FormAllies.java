package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;

/**
 * How other mobs treat a form, and what a form does for those around it:
 * <ul>
 *   <li>Creepers flee a cat or ocelot and phantoms won't dive at one, as with the real cats.</li>
 *   <li>Zombified piglins never turn on one of their own, and join in when something hurts it.</li>
 *   <li>An axolotl playing dead is ignored.</li>
 *   <li>A dolphin gives Dolphin's Grace to players swimming nearby.</li>
 *   <li>A strider walks on lava (sneak to sink).</li>
 *   <li>Another player with a bowl can milk a mooshroom for mushroom stew.</li>
 * </ul>
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class FormAllies {

    private static final float CREEPER_FLEE_DISTANCE = 6.0F;
    private static final double PACK_RADIUS = 20.0;
    private static final double DOLPHIN_GRACE_RADIUS = 10.0;
    private static final int DOLPHIN_GRACE_TICKS = 100;
    private static final double LAVA_FLOAT = 0.12;

    private FormAllies() {}

    private static boolean isCat(@Nullable LivingEntity entity) {
        EntityType<?> form = entity instanceof Player player ? Morphs.formOf(player).orElse(null) : null;
        return form == EntityType.CAT || form == EntityType.OCELOT;
    }

    private static boolean is(@Nullable LivingEntity entity, EntityType<?> form) {
        return entity instanceof Player player && Morphs.formOf(player).orElse(null) == form;
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) return;
        boolean afraid = (mob instanceof Creeper || mob instanceof Phantom) && isCat(target);
        boolean kin = mob instanceof ZombifiedPiglin && is(target, EntityType.ZOMBIFIED_PIGLIN);
        boolean fooled = target instanceof Player player && WaterMoves.isPlayingDead(player);
        if (afraid || kin || fooled) event.setCanceled(true);
    }

    /** Creepers get the same flee goal they have for real cats. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Creeper creeper)) return;
        creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, Player.class, CREEPER_FLEE_DISTANCE, 1.0, 1.2, FormAllies::isCat));
    }

    /** {@code ZombifiedPiglin#alertOthers}: hurt one of the pack and the pack comes for you. */
    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !is(player, EntityType.ZOMBIFIED_PIGLIN)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == player) return;
        AABB area = player.getBoundingBox().inflate(PACK_RADIUS);
        for (ZombifiedPiglin piglin : player.level().getEntitiesOfClass(ZombifiedPiglin.class, area, p -> p != attacker)) {
            piglin.setTarget(attacker);
            piglin.setPersistentAngerTarget(attacker.getUUID());
            piglin.startPersistentAngerTimer();
        }
    }

    /** {@code MushroomCow#mobInteract}: a bowl on a mooshroom gives mushroom stew. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player milker = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!held.is(Items.BOWL) || !is(event.getTarget() instanceof LivingEntity target ? target : null, EntityType.MOOSHROOM)) return;
        if (!milker.level().isClientSide()) {
            milker.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, milker, new ItemStack(Items.MUSHROOM_STEW), false));
            milker.level().playSound(null, event.getTarget().blockPosition(), SoundEvents.MOOSHROOM_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(milker.level().isClientSide()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == EntityType.STRIDER) walkOnLava(player);
        if (form == EntityType.DOLPHIN && player instanceof ServerPlayer server && server.tickCount % 20 == 0) shareGrace(server);
    }

    /**
     * {@code Strider#floatStrider}: in lava, rise to the surface and stay on it. Runs on both sides,
     * because the player's own client moves them.
     */
    private static void walkOnLava(Player player) {
        if (!player.isInLava() || player.isShiftKeyDown()) return;
        Vec3 motion = player.getDeltaMovement();
        boolean deep = player.getFluidHeight(FluidTags.LAVA) > player.getBbHeight() * 0.25;
        player.setDeltaMovement(motion.x, deep ? Math.max(motion.y, LAVA_FLOAT) : Math.max(motion.y, 0.0), motion.z);
        if (!deep) player.setOnGround(true);
        player.resetFallDistance();
    }

    /** {@code DolphinSwimWithPlayerGoal}: swimmers near a dolphin get Dolphin's Grace. */
    private static void shareGrace(ServerPlayer dolphin) {
        AABB area = dolphin.getBoundingBox().inflate(DOLPHIN_GRACE_RADIUS);
        for (Player swimmer : dolphin.level().getEntitiesOfClass(Player.class, area, p -> p != dolphin && p.isSwimming())) {
            swimmer.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, DOLPHIN_GRACE_TICKS), dolphin);
        }
    }
}
