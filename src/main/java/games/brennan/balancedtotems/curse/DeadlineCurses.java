package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
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
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
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
 * The two curses you must lift before they run out, or die:
 * <ul>
 *   <li><b>Zombified</b> (10 min): eat a golden apple, or take a potion of weakness (drunk, splashed
 *       or lingering).</li>
 *   <li><b>Rampage</b> (15 min): kill {@link BtRegistries#RAMPAGE_KILLS_NEEDED} players or entities
 *       in {@link BtRegistries#RAMPAGE_TARGETS} (PlayerMobs).</li>
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
        if (curesZombified(event.getItem())) cure(player, BtRegistries.ZOMBIFIED, "zombified");
    }

    /**
     * Splash and lingering potions, and tipped arrows, apply weakness with a source entity. Drinking
     * (handled above) and a Weakness Totem's own curse apply it with none, so a totem never cures.
     */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getEffectSource() == null || !event.getEffectInstance().is(MobEffects.WEAKNESS)) return;
        if (player.hasEffect(BtRegistries.ZOMBIFIED)) cure(player, BtRegistries.ZOMBIFIED, "zombified");
    }

    static boolean curesZombified(ItemStack stack) {
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return true;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(MobEffects.WEAKNESS)) return true;
        }
        return false;
    }

    // ---- Rampage ----

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!killer.hasEffect(BtRegistries.RAMPAGE)) return;
        LivingEntity victim = event.getEntity();
        if (victim == killer || !countsForRampage(victim)) return;

        int kills = killer.getData(BtRegistries.RAMPAGE_KILLS) + 1;
        killer.setData(BtRegistries.RAMPAGE_KILLS, kills);
        if (kills >= BtRegistries.RAMPAGE_KILLS_NEEDED) {
            cure(killer, BtRegistries.RAMPAGE, "rampage");
        } else {
            killer.displayClientMessage(Component.translatable("message.balancedtotems.rampage.progress",
                kills, BtRegistries.RAMPAGE_KILLS_NEEDED), true);
        }
    }

    private static boolean countsForRampage(LivingEntity victim) {
        return victim instanceof Player || victim.getType().is(BtRegistries.RAMPAGE_TARGETS);
    }

    private static void cure(ServerPlayer player, Holder<MobEffect> curse, String name) {
        player.removeEffect(curse);
        if (curse.is(BtRegistries.RAMPAGE)) player.setData(BtRegistries.RAMPAGE_KILLS, 0);
        PENDING_DEATHS.remove(player.getUUID());
        player.displayClientMessage(Component.translatable("message.balancedtotems." + name + ".lifted"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
