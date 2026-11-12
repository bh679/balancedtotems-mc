package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Vow of Peace: hurt anything while it lasts and you die. Any damage the game credits to you counts:
 * hits, projectiles, thrown potions, TNT you lit. The death comes on your next tick, not inside the hit.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class VowOfPeace {

    private VowOfPeace() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity()) return;
        if (attacker.hasEffect(BtRegistries.VOW_OF_PEACE)) DeadlineCurses.doom(attacker, BtRegistries.BROKEN_VOW);
    }
}
