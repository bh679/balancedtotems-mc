package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.net.MorphSyncPayload;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import games.brennan.balancedtotems.net.BtNetwork;

/** Hitbox, mob reactions, zombie sunburn, and keeping the form in step with its curse. */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class MorphEvents {

    private static final int ZOMBIE_CHECK_EVERY_TICKS = 20;
    private static final double GOLEM_RADIUS = 16.0;
    private static final int SUNBURN_SECONDS = 8;

    private MorphEvents() {}

    /** The form's hitbox and eye height, except while sleeping or dying. */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getPose() == Pose.SLEEPING || event.getPose() == Pose.DYING) return;
        Morphs.formOf(player).ifPresent(type -> event.setNewSize(type.getDimensions()));
    }

    /** Monsters leave a morphed player alone, unless the player hit them. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        if (ignoresMorphed(event.getEntity(), player)) event.setCanceled(true);
    }

    static boolean ignoresMorphed(LivingEntity attacker, Player player) {
        if (Morphs.formOf(player).isEmpty()) return false;
        if (!(attacker instanceof Enemy)) return false;
        return attacker.getLastHurtByMob() != player;
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && event.getEntity() instanceof ServerPlayer player && isFormCurse(instance)) {
            Morphs.onCurseEnded(player, instance.getEffect());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && event.getEntity() instanceof ServerPlayer player && isFormCurse(instance)) {
            Morphs.onCurseEnded(player, instance.getEffect());
        }
    }

    private static boolean isFormCurse(MobEffectInstance instance) {
        return instance.is(BtRegistries.MORPHED) || instance.is(BtRegistries.ZOMBIFIED);
    }

    /** Modifiers are transient, so a relog re-applies them; a form whose curse is gone is dropped. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (Morphs.formOf(player).isEmpty()) return;
        boolean cursed = player.hasEffect(BtRegistries.MORPHED) || player.hasEffect(BtRegistries.ZOMBIFIED);
        Morphs.setForm(player, cursed ? Morphs.formOf(player).get() : null);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!(event.getEntity() instanceof ServerPlayer tracker)) return;
        Morphs.formOf(target).ifPresent(type -> BtNetwork.sendTo(tracker,
            new MorphSyncPayload(target.getId(), target.getData(BtRegistries.FORM))));
    }

    /** A zombified player burns in daylight like a zombie and is hunted by the golems that hunt zombies. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % ZOMBIE_CHECK_EVERY_TICKS != 0) return;
        if (Morphs.formOf(player).orElse(null) != EntityType.ZOMBIE) return;
        if (burnsInSun(player)) player.igniteForSeconds(SUNBURN_SECONDS);
        AABB area = player.getBoundingBox().inflate(GOLEM_RADIUS);
        for (IronGolem golem : player.level().getEntitiesOfClass(IronGolem.class, area, g -> g.getTarget() == null)) {
            golem.setTarget(player);
        }
        for (SnowGolem golem : player.level().getEntitiesOfClass(SnowGolem.class, area, g -> g.getTarget() == null)) {
            golem.setTarget(player);
        }
    }

    /** Vanilla {@code Mob#isSunBurnTick}, minus the random roll (we check once a second). */
    private static boolean burnsInSun(Player player) {
        if (!player.level().isDay() || player.isCreative() || player.isSpectator()) return false;
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return false;
        if (player.isInWaterRainOrBubble() || player.isInPowderSnow || player.wasInPowderSnow) return false;
        if (player.getLightLevelDependentMagicValue() <= 0.5F) return false;
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return player.level().canSeeSky(eyes);
    }

}
