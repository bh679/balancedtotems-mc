package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;

/**
 * After a totem has saved you, its curse can't kill you: for the curse's after-save time, its own damage
 * (a broken rule, a missed Rage or Lonely timer, Breathless drowning) stops at half a heart. Each time it
 * does, the curse starts over at its full after-save time.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class SavedCurseShield {

    /** The least health a shielded curse leaves you: half a heart. */
    public static final float HALF_HEART = 1.0F;

    private static final Map<ResourceKey<DamageType>, Curse> CURSE_DAMAGE = Map.of(
        TotemRegistries.BROKEN_VOW, Curse.PACIFISM,
        TotemRegistries.BROKEN_DIET, Curse.MELON_DIET,
        TotemRegistries.BROKEN_RESTRAINT, Curse.RESTRAINT,
        TotemRegistries.RAGE_FAILED, Curse.RAGE,
        TotemRegistries.LONELINESS, Curse.LONELINESS,
        TotemRegistries.SUFFOCATION, Curse.BREATHLESSNESS);

    private SavedCurseShield() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Curse curse = event.getSource().typeHolder().unwrapKey().map(CURSE_DAMAGE::get).orElse(null);
        if (curse == null || !Curses.isSaved(player, curse)) return;
        float spare = Math.max(0.0F, player.getHealth() + player.getAbsorptionAmount() - HALF_HEART);
        if (event.getAmount() <= spare) return;
        event.setAmount(spare);
        MobEffectInstance current = player.getEffect(curse.effect());
        int amplifier = current == null ? curse.afterSaveAmplifier() : current.getAmplifier();
        Curses.apply(player, curse, amplifier, curse.afterSaveTicks());
        Curses.markSaved(player, curse, curse.afterSaveTicks());
        if (curse.isRule()) {
            player.displayClientMessage(Component.translatable("message.dungeontotems.spared",
                curse.effect().value().getDisplayName()), false);
        }
    }
}
