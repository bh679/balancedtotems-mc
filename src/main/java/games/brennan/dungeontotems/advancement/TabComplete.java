package games.brennan.dungeontotems.advancement;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.List;

/** Terms and Conditions Fulfilled: earned with every other advancement in the Dungeon Totems tab. */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class TabComplete {

    public static final ResourceLocation FULFILLED = DungeonTotems.id("terms_and_conditions_fulfilled");
    /** The rest of the tab. */
    public static final List<ResourceLocation> TAB = List.of(
        DungeonTotems.id("terms_and_conditions"), DungeonTotems.id("back_on_two_legs"), DungeonTotems.id("great_and_small"),
        DungeonTotems.id("brains_optional"), DungeonTotems.id("waiting_it_out"), DungeonTotems.id("promises_kept"),
        DungeonTotems.id("close_calls"));
    /** {@code terms_and_conditions_fulfilled.json}'s criterion, which only this grants. */
    private static final String CRITERION = "every_advancement";

    private TabComplete() {}

    @SubscribeEvent
    public static void onEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !TAB.contains(event.getAdvancement().id())) return;
        ServerAdvancementManager advancements = player.server.getAdvancements();
        AdvancementHolder fulfilled = advancements.get(FULFILLED);
        if (fulfilled == null) return;
        for (ResourceLocation id : TAB) {
            AdvancementHolder holder = advancements.get(id);
            if (holder == null || !player.getAdvancements().getOrStartProgress(holder).isDone()) return;
        }
        player.getAdvancements().award(fulfilled, CRITERION);
    }
}
