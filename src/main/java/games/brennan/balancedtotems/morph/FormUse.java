package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.net.BtNetwork;
import games.brennan.balancedtotems.net.MorphActionPayload;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Use" with an empty hand, as a form, does the mob's own extra move: a chicken lays an egg; an
 * armadillo rolls into its shell (it can't move, and hits hurt as they hurt a rolled-up armadillo) or
 * back out, and rolls up by itself when hurt; the rest are in {@link LandMoves} and {@link WaterMoves}.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class FormUse {

    private static final int EGG_COOLDOWN_TICKS = 100;
    /** How long an armadillo stays rolled up after being hurt. */
    private static final int ARMADILLO_SCARED_TICKS = 60;
    private static final ResourceLocation ROLLED_SPEED_ID = BalancedTotems.id("morph.rolled_speed");
    private static final ResourceLocation ROLLED_JUMP_ID = BalancedTotems.id("morph.rolled_jump");

    private static final Map<UUID, Long> LAST_EGG = new ConcurrentHashMap<>();
    private static final Set<UUID> ROLLED = ConcurrentHashMap.newKeySet();

    private FormUse() {}

    /** From the {@link games.brennan.balancedtotems.net.FormUsePayload} handler. */
    public static void use(ServerPlayer player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null) return;
        if (form == EntityType.CHICKEN) layEgg(player);
        else if (form == EntityType.ARMADILLO) setRolled(player, !ROLLED.contains(player.getUUID()));
        else if (form == EntityType.CAMEL) LandMoves.dash(player);
        else if (form == EntityType.CAT) LandMoves.sit(player);
        else if (form == EntityType.BAT) LandMoves.hang(player);
        else if (form == EntityType.FROG) LandMoves.croak(player);
        else if (form == EntityType.SNIFFER) LandMoves.dig(player);
        else if (form == EntityType.SNOW_GOLEM) LandMoves.throwSnowball(player);
        else if (form == EntityType.TURTLE) LandMoves.layTurtleEgg(player);
        else if (form == EntityType.PARROT) LandMoves.imitate(player);
        else if (form == EntityType.AXOLOTL) WaterMoves.playDead(player);
        else if (form == EntityType.PUFFERFISH) WaterMoves.puff(player);
        else if (form == EntityType.SQUID || form == EntityType.GLOW_SQUID) WaterMoves.ink(player);
        else if (form == EntityType.DOLPHIN) WaterMoves.leap(player);
    }

    /** Forms with a "use" move (the client only sends "use" for these). */
    public static final Set<EntityType<?>> USABLE = Set.of(EntityType.CHICKEN, EntityType.ARMADILLO, EntityType.CAMEL,
        EntityType.CAT, EntityType.BAT, EntityType.FROG, EntityType.SNIFFER, EntityType.SNOW_GOLEM, EntityType.TURTLE,
        EntityType.PARROT, EntityType.AXOLOTL, EntityType.PUFFERFISH, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.DOLPHIN);

    /** {@code Chicken#aiStep}: an egg drops with the chicken's plop. */
    private static void layEgg(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now - LAST_EGG.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < EGG_COOLDOWN_TICKS) return;
        LAST_EGG.put(player.getUUID(), now);
        float pitch = (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F;
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, pitch);
        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.EGG)));
    }

    private static void setRolled(ServerPlayer player, boolean rolled) {
        if (rolled) ROLLED.add(player.getUUID());
        else ROLLED.remove(player.getUUID());
        hold(player, Attributes.MOVEMENT_SPEED, ROLLED_SPEED_ID, rolled);
        hold(player, Attributes.JUMP_STRENGTH, ROLLED_JUMP_ID, rolled);
        player.level().playSound(null, player.blockPosition(),
            rolled ? SoundEvents.ARMADILLO_ROLL : SoundEvents.ARMADILLO_UNROLL_FINISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        FormAction action = rolled ? FormAction.ROLL_UP : FormAction.UNROLL;
        BtNetwork.sendToTrackingAndSelf(player, new MorphActionPayload(player.getId(), action.ordinal()));
    }

    /** Pins an attribute to nothing (rolled up, playing dead), or lets it go. */
    static void hold(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, boolean held) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (held) instance.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** {@code Armadillo#hurt}: a rolled-up armadillo takes {@code (damage - 1) / 2}. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ROLLED.contains(player.getUUID())) {
            event.setAmount(Math.max(0.0F, (event.getAmount() - 1.0F) / 2.0F));
        }
    }

    /** Hurt, an armadillo rolls up for a moment and a squid inks, as the mobs do. */
    @SubscribeEvent
    public static void onHurtAfter(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F || !player.isAlive()) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == EntityType.ARMADILLO && !ROLLED.contains(player.getUUID())) {
            setRolled(player, true);
            FormTimers.later(player, ARMADILLO_SCARED_TICKS, rolled -> {
                if (ROLLED.contains(rolled.getUUID())) setRolled(rolled, false);
            });
        } else if (form == EntityType.SQUID || form == EntityType.GLOW_SQUID) {
            WaterMoves.ink(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) WaterMoves.tickPuffed(player);
    }

    /** From {@link Morphs#setForm}: changing form unrolls. */
    static void onFormChanged(ServerPlayer player, @Nullable EntityType<?> form) {
        if (ROLLED.contains(player.getUUID())) setRolled(player, false);
        WaterMoves.onFormChanged(player);
    }

    /** Test seam. */
    public static boolean isRolled(ServerPlayer player) {
        return ROLLED.contains(player.getUUID());
    }
}
