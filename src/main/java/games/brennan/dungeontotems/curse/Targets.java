package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Who Rage and Lonely count. With PlayerMob installed, PlayerMobs (and, for Rage, players). Without it,
 * villagers stand in, except under Dungeon Train, whose villagers are never fair game.
 */
public final class Targets {

    public static final String PLAYERMOB = "playermob";
    public static final String DUNGEON_TRAIN = "dungeontrain";

    private Targets() {}

    /** Whether villagers stand in for PlayerMobs, given which mods are installed. */
    public static boolean villagersStandIn(boolean playerMob, boolean dungeonTrain) {
        return !playerMob && !dungeonTrain;
    }

    public static boolean villagersStandIn() {
        ModList mods = ModList.get();
        return villagersStandIn(mods.isLoaded(PLAYERMOB), mods.isLoaded(DUNGEON_TRAIN));
    }

    /** A kill that feeds Rage: a player, a Rage target (PlayerMob), or a villager when they stand in. */
    public static boolean countsForRage(LivingEntity victim) {
        if (victim instanceof Player || victim.getType().is(TotemRegistries.RAMPAGE_TARGETS)) return true;
        return victim instanceof Villager && villagersStandIn();
    }
}
