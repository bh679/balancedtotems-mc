package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.compat.PlayerMobGifts;
import games.brennan.dungeontotems.net.TotemNetwork;
import games.brennan.dungeontotems.net.CurseCountPayload;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * The counted curses: Rage counts kills, Lonely counts friends. Each needs
 * {@link TotemRegistries#RAMPAGE_KILLS_NEEDED} per level; reaching it lifts the curse. The count lives
 * on the player and is mirrored to their client for the hover text.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class Tallies {

    private Tallies() {}

    public static boolean counted(Curse curse) {
        return curse == Curse.RAMPAGE || curse == Curse.LONELINESS;
    }

    /** How many this curse instance needs: three per level. */
    public static int needed(MobEffectInstance instance) {
        return TotemRegistries.RAMPAGE_KILLS_NEEDED * (instance.getAmplifier() + 1);
    }

    public static int count(Player player, Curse curse) {
        return player.getData(counter(curse));
    }

    /** Sets the count and tells the player's client. */
    public static void set(ServerPlayer player, Curse curse, int count) {
        player.setData(counter(curse), count);
        TotemNetwork.sendTo(player, new CurseCountPayload(curse.name(), count));
    }

    /** A fresh curse starts from nothing: no kills, no friends. */
    public static void reset(ServerPlayer player, Curse curse) {
        set(player, curse, 0);
        if (curse == Curse.LONELINESS) {
            player.setData(TotemRegistries.LONELY_FRIEND_IDS, List.of());
            PlayerMobGifts.forget(player);
        }
    }

    /** One more kill or friend ({@code who}): lifts the curse at its target, or says so in chat. */
    public static void add(ServerPlayer player, Curse curse, Component who) {
        MobEffectInstance instance = player.getEffect(curse.effect());
        if (instance == null) return;
        int count = count(player, curse) + 1;
        int needed = needed(instance);
        set(player, curse, count);
        String name = curse == Curse.RAMPAGE ? "rampage" : "lonely";
        if (count >= needed) {
            DeadlineCurses.cure(player, curse.effect(), name);
            reset(player, curse);
        } else {
            player.displayClientMessage(Component.translatable("message.dungeontotems." + name + ".progress", who, count, needed), false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (Curse curse : List.of(Curse.RAMPAGE, Curse.LONELINESS)) set(player, curse, count(player, curse));
    }

    private static Supplier<AttachmentType<Integer>> counter(Curse curse) {
        return curse == Curse.LONELINESS ? TotemRegistries.LONELY_FRIENDS : TotemRegistries.RAMPAGE_KILLS;
    }
}
