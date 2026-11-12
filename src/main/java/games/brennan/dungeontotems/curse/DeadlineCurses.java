package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Curses that end in death. <b>Zombified</b> (10 min): eat a golden apple while you have Weakness, like
 * curing a zombie villager, or die when it runs out. Any Weakness counts: a potion, a splash, or a
 * Totem of Weakness's curse. Also the rule curses' doom ({@link #doom}), which after a save only takes
 * you to half a heart ({@link SavedCurseShield}). The death comes on your next tick, never inside the
 * effect-tick loop or event that caused it.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class DeadlineCurses {

    private static final Map<UUID, ResourceKey<DamageType>> PENDING_DEATHS = new ConcurrentHashMap<>();

    private DeadlineCurses() {}

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (instance.is(TotemRegistries.ZOMBIFIED)) PENDING_DEATHS.put(player.getUUID(), TotemRegistries.ZOMBIFICATION);
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
        if (!player.hasEffect(TotemRegistries.ZOMBIFIED)) return;
        if (isGoldenApple(event.getItem()) && player.hasEffect(MobEffects.WEAKNESS)) {
            cure(player, TotemRegistries.ZOMBIFIED, "zombified");
        }
    }

    static boolean isGoldenApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    /** Lifts {@code curse}: no more deadline, a message and a sound. */
    static void cure(ServerPlayer player, Holder<MobEffect> curse, String name) {
        player.removeEffect(curse);
        // Only this curse's own deadline: a broken vow in the same tick still kills.
        PENDING_DEATHS.remove(player.getUUID(), TotemRegistries.ZOMBIFICATION);
        player.displayClientMessage(Component.translatable("message.dungeontotems." + name + ".lifted"), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
