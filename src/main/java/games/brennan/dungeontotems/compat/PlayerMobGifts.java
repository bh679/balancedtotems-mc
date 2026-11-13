package games.brennan.dungeontotems.compat;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.curse.LonelyFriends;
import games.brennan.dungeontotems.curse.Targets;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lonely's PlayerMob friends: a PlayerMob you gave a gift to and got a gift from while Lonely.
 *
 * <p>PlayerMob announces gifts through {@code PlayerMobSocialHooks}, which holds one observer, and
 * Dungeon Train installs its own at boot. So, with no compile dependency on PlayerMob, this reads the
 * current observer by reflection once the server has started and installs a proxy that calls it
 * first, then records the gift. If PlayerMob's classes change, it logs and PlayerMob gifts stop
 * counting; nothing else breaks.</p>
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class PlayerMobGifts {

    private static final String HOOKS = "games.brennan.playermob.compat.PlayerMobSocialHooks";

    private static final Map<UUID, Set<UUID>> GIVEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> RECEIVED = new ConcurrentHashMap<>();
    private static volatile boolean installed;

    private PlayerMobGifts() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (installed || !ModList.get().isLoaded(Targets.PLAYERMOB)) return;
        try {
            Class<?> hooks = Class.forName(HOOKS);
            Class<?> observerType = Class.forName(HOOKS + "$GiftObserver");
            Field current = hooks.getDeclaredField("observer");
            current.setAccessible(true);
            Object previous = current.get(null);
            Object chained = Proxy.newProxyInstance(observerType.getClassLoader(), new Class<?>[] {observerType},
                chain(previous));
            hooks.getMethod("install", observerType).invoke(null, chained);
            installed = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            DungeonTotems.LOGGER.warn("PlayerMob gifts won't count for Lonely: PlayerMob's gift hook has changed", e);
        }
    }

    /** Calls the observer that was there before, then records the gift. */
    private static InvocationHandler chain(Object previous) {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return objectMethod(proxy, method, args);
            Object result = method.invoke(previous, args);
            if (args != null && args.length == 2 && args[0] instanceof ServerPlayer player && args[1] instanceof UUID mob) {
                if (method.getName().equals("onPlayerGift")) record(player, mob, GIVEN, RECEIVED);
                if (method.getName().equals("onMobGift")) record(player, mob, RECEIVED, GIVEN);
            }
            return result;
        };
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> "DungeonTotems PlayerMob gift observer";
        };
    }

    /** Notes one direction of a gift; once both directions happened with the same mob, it is a friend. */
    static void record(ServerPlayer player, UUID mob, Map<UUID, Set<UUID>> direction, Map<UUID, Set<UUID>> other) {
        if (!player.hasEffect(TotemRegistries.LONELY)) return;
        direction.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet()).add(mob);
        if (other.getOrDefault(player.getUUID(), Set.of()).contains(mob)) LonelyFriends.befriend(player, mob);
    }

    /** A fresh Lonely curse: earlier gifts no longer count. */
    public static void forget(ServerPlayer player) {
        GIVEN.remove(player.getUUID());
        RECEIVED.remove(player.getUUID());
    }

    /** Test seam: a gift between {@code player} and {@code mob}, as PlayerMob would announce it. */
    public static void gift(ServerPlayer player, UUID mob, boolean fromPlayer) {
        if (fromPlayer) record(player, mob, GIVEN, RECEIVED);
        else record(player, mob, RECEIVED, GIVEN);
    }
}
