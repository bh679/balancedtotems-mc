package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * The Vow of Peace: hurt anything while it lasts and you die. Any damage the game credits to you counts:
 * hits, projectiles, thrown potions, TNT you lit. The death comes on your next tick, not inside the hit.
 */
@Mod.EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class VowOfPeace {

    private VowOfPeace() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity()) return;
        if (attacker.hasEffect(BtRegistries.VOW_OF_PEACE.get())) DeadlineCurses.doom(attacker, BtRegistries.BROKEN_VOW);
    }
}
