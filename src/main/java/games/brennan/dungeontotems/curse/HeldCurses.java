package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.TotemItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;

/**
 * Held curses: a totem of {@link Curse.Kind#HELD} curses you while it is in either hand. The curse goes
 * on with no timer and comes off once you let go, or turns into its shorter let-go curse (Rage, Lonely).
 * A timed curse already on you at a higher level (after a save, say) is left to run out first.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class HeldCurses {

    /** How often hands are checked, in ticks. */
    public static final int CHECK_TICKS = 10;

    private HeldCurses() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        if (player.tickCount % CHECK_TICKS == 0) update(player);
    }

    /** Puts on what you hold and takes off what you let go of. */
    public static void update(ServerPlayer player) {
        for (Curse curse : Curse.values()) {
            if (curse.isHeld()) update(player, curse, heldTotem(player, curse) != null);
        }
    }

    private static void update(ServerPlayer player, Curse curse, boolean holding) {
        MobEffectInstance current = player.getEffect(curse.effect());
        boolean fromHolding = current != null && current.isInfiniteDuration() && Curses.curseOf(current) == curse;
        if (holding) {
            if (fromHolding || (current != null && Curses.curseOf(current) == curse && current.getAmplifier() > curse.heldAmplifier())) return;
            Curses.apply(player, curse, curse.heldAmplifier(), MobEffectInstance.INFINITE_DURATION);
        } else if (fromHolding) {
            if (curse.letGoTicks() > 0) {
                Curses.apply(player, curse, curse.letGoAmplifier(), curse.letGoTicks());
            } else {
                player.removeEffect(curse.effect());
            }
        }
    }

    /** The totem of {@code curse} in either hand, main hand first, or null. */
    @Nullable
    public static ItemStack heldTotem(ServerPlayer player, Curse curse) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof TotemItem totem && totem.curse() == curse) return stack;
        }
        return null;
    }
}
