package games.brennan.dungeontotems;

import com.mojang.logging.LogUtils;
import games.brennan.dungeontotems.client.TimerCurseIcons;
import games.brennan.dungeontotems.loot.EvokerTotemLootModifier;
import games.brennan.dungeontotems.net.TotemNetwork;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Dungeon Totems: Totems of Undying that save you like the vanilla totem, then leave a curse.
 *
 * <p>Each totem is a {@link Curse}. The pop itself is {@link TotemPop}, triggered by
 * {@link TotemTriggerEvents}; the two body-changing curses share the morph engine in
 * {@code morph/}; the Void Totem's landing spot comes from {@link games.brennan.dungeontotems.api.DungeonTotemsApi}.</p>
 */
@Mod(DungeonTotems.MOD_ID)
public final class DungeonTotems {

    public static final String MOD_ID = "dungeontotems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DungeonTotems(IEventBus modBus, ModContainer container) {
        TotemRegistries.register(modBus);
        EvokerTotemLootModifier.register(modBus);
        modBus.addListener(TotemNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) modBus.addListener(TimerCurseIcons::onRegister);
        LOGGER.info("Dungeon Totems loaded");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
