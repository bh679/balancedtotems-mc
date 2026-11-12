package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.effect.CurseEffect;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which effects are curses, and what may lift them. On 1.21.1 this was NeoForge's effect cures;
 * 26.x dropped those, so milk, {@code /effect clear} and a totem pop all clear every effect and fire a
 * cancellable {@link MobEffectEvent.Remove} per effect. The rules stay the same:
 * <ul>
 *   <li>A totem pop (any totem, vanilla included) never lifts a curse.</li>
 *   <li>Milk leaves the uncurable curses (Morphed, Zombified, Rage, Lonely, the vows) and lifts the rest.</li>
 *   <li>Anything else (our own cures, commands) lifts any curse.</li>
 * </ul>
 * Our own effects are always curses. A vanilla effect (Weakness, Mining Fatigue, Blindness) is a curse
 * only while the player carries its mark from {@link Curses#apply}, so a Weakness potion stays a potion.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class CurseGuard {

    /** Players whose totem pop is clearing effects this game tick. */
    private static final Map<UUID, Long> POPPING = new ConcurrentHashMap<>();

    private CurseGuard() {}

    /** The curse this effect is on {@code entity}, or null when it is not one. */
    @Nullable
    public static Curse curseOf(LivingEntity entity, Holder<MobEffect> effect) {
        Curse curse = Curse.forEffect(effect);
        if (curse == null) return null;
        if (effect.value() instanceof CurseEffect) return curse;
        return entity.getData(BtRegistries.CURSE_MARKS).contains(curse.name()) ? curse : null;
    }

    /** Marks {@code curse}'s effect on the player as a curse rather than an ordinary effect. */
    static void mark(ServerPlayer player, Curse curse) {
        if (curse.effect() == null || curse.effect().value() instanceof CurseEffect) return;
        List<String> marks = player.getData(BtRegistries.CURSE_MARKS);
        if (marks.contains(curse.name())) return;
        List<String> updated = new ArrayList<>(marks);
        updated.add(curse.name());
        player.setData(BtRegistries.CURSE_MARKS, List.copyOf(updated));
    }

    private static void unmark(LivingEntity entity, Curse curse) {
        List<String> marks = entity.getData(BtRegistries.CURSE_MARKS);
        if (!marks.contains(curse.name())) return;
        entity.setData(BtRegistries.CURSE_MARKS, marks.stream().filter(m -> !m.equals(curse.name())).toList());
    }

    /** Clears every effect but the curses, as a totem pop does. */
    public static void clearForTotem(ServerPlayer player) {
        POPPING.put(player.getUUID(), player.level().getGameTime());
        player.removeAllEffects();
    }

    /** Vanilla's own totem clears effects right after this event. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVanillaTotem(LivingUseTotemEvent event) {
        if (event.isCanceled()) return;
        POPPING.put(event.getEntity().getUUID(), event.getEntity().level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRemove(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        Curse curse = curseOf(entity, event.getEffect());
        if (curse == null) return;
        if (popping(entity) || (drinkingMilk(entity) && !milkLifts(event.getEffect()))) {
            event.setCanceled(true);
        }
    }

    /** A curse that has gone (lifted or run out) stops marking that effect. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) return;
        Curse curse = Curse.forEffect(event.getEffect());
        if (curse != null) unmark(event.getEntity(), curse);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExpired(MobEffectEvent.Expired event) {
        if (event.isCanceled() || event.getEffectInstance() == null) return;
        Curse curse = Curse.forEffect(event.getEffectInstance().getEffect());
        if (curse != null) unmark(event.getEntity(), curse);
    }

    private static boolean popping(LivingEntity entity) {
        Long tick = POPPING.get(entity.getUUID());
        return tick != null && tick == entity.level().getGameTime();
    }

    /** Milk, or anything else drunk or eaten that clears every effect. */
    private static boolean drinkingMilk(LivingEntity entity) {
        if (!entity.isUsingItem()) return false;
        Consumable consumable = entity.getUseItem().get(DataComponents.CONSUMABLE);
        return consumable != null && consumable.onConsumeEffects().stream()
            .anyMatch(e -> e instanceof ClearAllStatusEffectsConsumeEffect);
    }

    private static boolean milkLifts(Holder<MobEffect> effect) {
        return !(effect.value() instanceof CurseEffect curseEffect) || curseEffect.curable();
    }
}
