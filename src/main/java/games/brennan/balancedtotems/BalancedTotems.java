package games.brennan.balancedtotems;

import com.mojang.logging.LogUtils;
import games.brennan.balancedtotems.loot.EvokerTotemLootModifier;
import games.brennan.balancedtotems.net.BtNetwork;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Balanced Totems: Totems of Undying that save you like the vanilla totem, then leave a curse.
 *
 * <p>Each totem is a {@link Curse}. The pop itself is {@link TotemPop}, triggered by
 * {@link TotemTriggerEvents}; the two body-changing curses share the morph engine in
 * {@code morph/}; the Void Totem's landing spot comes from {@link games.brennan.balancedtotems.api.BalancedTotemsApi}.</p>
 */
@Mod(BalancedTotems.MOD_ID)
public final class BalancedTotems {

    public static final String MOD_ID = "balancedtotems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BalancedTotems() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BtRegistries.register(modBus);
        EvokerTotemLootModifier.register(modBus);
        BtNetwork.register();
        LOGGER.info("Balanced Totems loaded");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
