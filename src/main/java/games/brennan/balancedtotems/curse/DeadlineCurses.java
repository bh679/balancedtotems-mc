package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.net.BtNetwork;
import games.brennan.balancedtotems.net.RampageKillsPayload;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The two curses you must lift before they run out, or die:
 * <ul>
 *   <li><b>Zombified</b> (10 min): eat a golden apple while you have Weakness, like curing a zombie
 *       villager. Any Weakness counts: a potion, a splash, or a Totem of Weakness's curse.</li>
 *   <li><b>Rampage</b> (15 min): kill {@link BtRegistries#RAMPAGE_KILLS_NEEDED} players or entities
 *       in {@link BtRegistries#RAMPAGE_TARGETS} (PlayerMobs) per level.</li>
 * </ul>
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
        MobEffectInstance rampage = killer.getEffect(BtRegistries.RAMPAGE);
        if (rampage == null) return;
        LivingEntity victim = event.getEntity();
        if (victim == killer || !countsForRampage(victim)) return;

        int kills = killer.getData(BtRegistries.RAMPAGE_KILLS) + 1;
        int needed = killsNeeded(rampage);
        setRampageKills(killer, kills);
        if (kills >= needed) {
            cure(killer, BtRegistries.RAMPAGE, "rampage");
        } else {
            killer.displayClientMessage(Component.translatable("message.balancedtotems.rampage.progress", kills, needed), true);
        }
    }

    /** Kills that lift this Rampage: {@link BtRegistries#RAMPAGE_KILLS_NEEDED} per level. */
    public static int killsNeeded(MobEffectInstance rampage) {
        return BtRegistries.RAMPAGE_KILLS_NEEDED * (rampage.getAmplifier() + 1);
    }

    /** Sets the kill count and tells the player's client (it shows what is left on hover). */
    public static void setRampageKills(ServerPlayer player, int kills) {
        player.setData(BtRegistries.RAMPAGE_KILLS, kills);
        BtNetwork.sendTo(player, new RampageKillsPayload(kills));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) setRampageKills(player, player.getData(BtRegistries.RAMPAGE_KILLS));
    }

    private static boolean countsForRampage(LivingEntity victim) {
        return victim instanceof Player || victim.getType().is(BtRegistries.RAMPAGE_TARGETS);
    }

    private static void cure(ServerPlayer player, Holder<MobEffect> curse, String name) {
        player.removeEffect(curse);
        if (curse.is(BtRegistries.RAMPAGE)) setRampageKills(player, 0);
        PENDING_DEATHS.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("message.balancedtotems." + name + ".lifted"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
