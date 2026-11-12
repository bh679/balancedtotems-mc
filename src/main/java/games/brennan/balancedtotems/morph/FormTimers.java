package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.net.BtNetwork;
import games.brennan.balancedtotems.net.MorphActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Cooldowns and later steps for form moves, per player and in game ticks. A later step is dropped if the
 * player changes form before it runs, so a move never finishes on the wrong body.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class FormTimers {

    private record Later(long at, Consumer<ServerPlayer> step) {}

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Later>> LATER = new ConcurrentHashMap<>();

    private FormTimers() {}

    /** True (and starts the cooldown) when {@code move} is ready; false while it is cooling down. */
    static boolean ready(ServerPlayer player, String move, int cooldownTicks) {
        long now = player.level().getGameTime();
        Map<String, Long> cooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        if (now < cooldowns.getOrDefault(move, Long.MIN_VALUE)) return false;
        cooldowns.put(move, now + cooldownTicks);
        return true;
    }

    /** Runs {@code step} in {@code ticks} ticks, if the player keeps this form until then. */
    static void later(ServerPlayer player, int ticks, Consumer<ServerPlayer> step) {
        LATER.computeIfAbsent(player.getUUID(), id -> new ArrayList<>())
            .add(new Later(player.level().getGameTime() + ticks, step));
    }

    /** Shows a form's action on every client watching the player. */
    static void show(ServerPlayer player, FormAction action) {
        BtNetwork.sendToTrackingAndSelf(player, new MorphActionPayload(player.getId(), action.ordinal()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<Later> pending = LATER.get(player.getUUID());
        if (pending == null || pending.isEmpty()) return;
        long now = player.level().getGameTime();
        List<Later> due = pending.stream().filter(later -> later.at() <= now).toList();
        pending.removeAll(due);
        due.forEach(later -> later.step().accept(player));
    }

    /** From {@link Morphs#setForm}: pending steps belong to the old form. */
    static void onFormChanged(ServerPlayer player) {
        LATER.remove(player.getUUID());
        COOLDOWNS.remove(player.getUUID()); // a new animal hasn't used its moves yet
    }
}
