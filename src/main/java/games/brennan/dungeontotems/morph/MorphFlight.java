package games.brennan.dungeontotems.morph;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;

/**
 * How a form moves through the air. A flying form ({@link TotemRegistries#FLYING_FORMS}: bee, parrot, bat,
 * allay) is always airborne: creative-style flight that never lands, so no walking or jumping. (A
 * chicken's flutter is a {@link FormTraits} effect.)
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class MorphFlight {

    private MorphFlight() {}

    public static boolean flies(@Nullable EntityType<?> form) {
        return form != null && form.is(TotemRegistries.FLYING_FORMS);
    }

    /** From {@link Morphs#setForm}: start or stop flight for the new form. */
    static void onFormChanged(ServerPlayer player, @Nullable EntityType<?> previous, @Nullable EntityType<?> form) {
        Abilities abilities = player.getAbilities();
        if (flies(form)) {
            abilities.mayfly = true;
            abilities.flying = true;
            player.onUpdateAbilities();
        } else if (flies(previous)) {
            // Back to what the game mode gives: creative and spectator players keep their flight.
            player.gameMode.getGameModeForPlayer().updatePlayerAbilities(abilities);
            player.onUpdateAbilities();
        }
    }

    /** Vanilla stops flight when you touch the ground; a flying form takes off again at once. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!flies(Morphs.formOf(player).orElse(null))) return;
        Abilities abilities = player.getAbilities();
        if (abilities.flying && abilities.mayfly) return;
        abilities.mayfly = true;
        abilities.flying = true;
        if (player instanceof ServerPlayer server) server.onUpdateAbilities();
    }
}
