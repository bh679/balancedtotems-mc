package games.brennan.dungeontotems;

import games.brennan.dungeontotems.voidtotem.VoidRescue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Fires a held Dungeon Totem when a player would die.
 *
 * <p>Vanilla's totem runs first, inside {@code LivingEntity#hurt}, so a held vanilla totem always
 * wins and {@code die()} is never reached. Ours cancels the death at HIGHEST priority, as the
 * vanilla totem effectively does, so nothing downstream reports a death that didn't happen.</p>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class TotemTriggerEvents {

    private TotemTriggerEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof TotemItem totem)) continue;
            if (!saves(totem.curse(), source)) continue;

            event.setCanceled(true);
            TotemPop.pop(player, held, totem.curse());
            if (totem.curse() == Curse.VOID) VoidRescue.rescue(player);
            return;
        }
    }

    /** Whether a totem with this curse saves you from this death. */
    static boolean saves(Curse curse, DamageSource source) {
        if (curse.isNarrow()) return curse.narrowlySaves(source);
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
