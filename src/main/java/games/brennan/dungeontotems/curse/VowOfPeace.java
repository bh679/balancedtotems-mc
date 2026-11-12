package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Vow of Peace: hurt anything while it lasts and you die. Any damage the game credits to you counts:
 * hits, projectiles, thrown potions, TNT you lit. The death comes on your next tick, not inside the hit.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class VowOfPeace {

    private VowOfPeace() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity()) return;
        if (attacker.hasEffect(TotemRegistries.VOW_OF_PEACE)) DeadlineCurses.doom(attacker, TotemRegistries.BROKEN_VOW);
    }
}
