package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The three curses you must lift before they run out, or die:
 * <ul>
 *   <li><b>Zombified</b> (10 min): eat a golden apple while you have Weakness, like curing a zombie
 *       villager. Any Weakness counts: a potion, a splash, or a Totem of Weakness's curse.</li>
 *   <li><b>Rampage</b> (15 min): kill {@link BtRegistries#RAMPAGE_KILLS_NEEDED} players or entities
 *       in {@link BtRegistries#RAMPAGE_TARGETS} (PlayerMobs; villagers stand in, see {@link Targets}) per
 *       level.</li>
 *   <li><b>Lonely</b> (10 min): make three friends per level ({@link LonelyFriends}).</li>
 * </ul>
 * Rage and Lonely keep their counts in {@link Tallies}.
 * Lifting a curse removes its effect; running out kills you on the next tick (deferred so the kill
 * never lands inside the effect-tick loop that reported the expiry).
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class DeadlineCurses {

    private static final Map<UUID, ResourceKey<DamageType>> PENDING_DEATHS = new ConcurrentHashMap<>();

    private DeadlineCurses() {}

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (instance.is(BtRegistries.ZOMBIFIED)) PENDING_DEATHS.put(player.getUUID(), BtRegistries.ZOMBIFICATION);
        if (instance.is(BtRegistries.RAMPAGE)) PENDING_DEATHS.put(player.getUUID(), BtRegistries.RAMPAGE_FAILED);
        if (instance.is(BtRegistries.LONELY)) PENDING_DEATHS.put(player.getUUID(), BtRegistries.LONELINESS);
    }

    /** Kills the player on their next tick with {@code cause}, outside whatever event is running now. */
    public static void doom(ServerPlayer player, ResourceKey<DamageType> cause) {
        PENDING_DEATHS.put(player.getUUID(), cause);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceKey<DamageType> cause = PENDING_DEATHS.remove(player.getUUID());
        if (cause == null || !player.isAlive()) return;
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(cause);
        player.hurt(new DamageSource(type), Float.MAX_VALUE);
    }

    // ---- Zombified ----

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.hasEffect(BtRegistries.ZOMBIFIED)) return;
        if (isGoldenApple(event.getItem()) && player.hasEffect(MobEffects.WEAKNESS)) {
            cure(player, BtRegistries.ZOMBIFIED, "zombified");
        }
    }

    static boolean isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    // ---- Rampage ----

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!killer.hasEffect(BtRegistries.RAMPAGE)) return;
        LivingEntity victim = event.getEntity();
        if (victim != killer && Targets.countsForRage(victim)) Tallies.add(killer, Curse.RAMPAGE);
    }

    /** Lifts {@code curse}: no more deadline, a message and a sound. */
    static void cure(ServerPlayer player, Holder<MobEffect> curse, String name) {
        player.removeEffect(curse);
        // Only this curse's own deadline: a broken vow in the same tick still kills.
        ResourceKey<DamageType> deadline = curse.is(BtRegistries.ZOMBIFIED) ? BtRegistries.ZOMBIFICATION
            : curse.is(BtRegistries.RAMPAGE) ? BtRegistries.RAMPAGE_FAILED : BtRegistries.LONELINESS;
        PENDING_DEATHS.remove(player.getUUID(), deadline);
        player.displayClientMessage(Component.translatable("message.balancedtotems." + name + ".lifted"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
