package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How water forms breathe and get about on land, as their mobs do:
 * <ul>
 *   <li>Fish, tadpoles and squid ({@code WaterAnimal#handleAirSupply}): out of water they have 15
 *       seconds of air, then take 2 damage each second until they're back in.</li>
 *   <li>Axolotls ({@code Axolotl#handleAirSupply}): 5 minutes out of water; rain and bubble columns
 *       keep them wet. Then they dry out, 2 damage a second.</li>
 *   <li>Dolphins breathe air: under water their air runs out over 4 minutes and they drown unless they
 *       surface; on land they dry out over 2 minutes ({@code Dolphin#tick}), then 1 damage a tick.</li>
 *   <li>Fish, tadpoles, squid and dolphins can't walk on land; fish and dolphins flop about
 *       ({@code client/FormFlop}).</li>
 * </ul>
 * The air bar shows whatever's running out, scaled to its 10 bubbles.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class WaterLife {

    /** {@code WaterAnimal}: 300 ticks of air out of water; {@code Axolotl}: 6000; {@code Dolphin}: 4800 under water, moist for 2400 on land. */
    private static final int FISH_AIR = 300;
    private static final int AXOLOTL_AIR = 6000;
    private static final int DOLPHIN_AIR = 4800;
    private static final int DOLPHIN_MOISTURE = 2400;
    /** {@code handleAirSupply}: the air counter goes this far below zero before each 2 damage. */
    private static final int GASP = 20;
    private static final float GASP_DAMAGE = 2.0F;
    private static final float DRY_DOLPHIN_DAMAGE = 1.0F;

    /** Forms that can't walk on land. */
    private static final Set<EntityType<?>> LANDLOCKED = Set.of(EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH,
        EntityType.PUFFERFISH, EntityType.TADPOLE, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.DOLPHIN);
    /** {@code AbstractFish#getFlopSound} and the dolphin's: the slap of a fish out of water. */
    private static final Map<EntityType<?>, SoundEvent> FLOP_SOUNDS = Map.of(
        EntityType.COD, SoundEvents.COD_FLOP, EntityType.SALMON, SoundEvents.SALMON_FLOP,
        EntityType.TROPICAL_FISH, SoundEvents.TROPICAL_FISH_FLOP, EntityType.PUFFERFISH, SoundEvents.PUFFER_FISH_FLOP,
        EntityType.TADPOLE, SoundEvents.TADPOLE_FLOP, EntityType.DOLPHIN, SoundEvents.DOLPHIN_JUMP);

    /** Ticks of air (or a dolphin's moisture) left; below zero is gasping. */
    private static final Map<UUID, Integer> AIR = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MOISTURE = new ConcurrentHashMap<>();

    private WaterLife() {}

    /** Whether the form can't walk on land (it flops or lies there). */
    public static boolean isLandlocked(@Nullable EntityType<?> form) {
        return form != null && LANDLOCKED.contains(form);
    }

    /** Whether the form flops on land, and how hard: fish 0.4 up, dolphins 0.5 (null: it doesn't). */
    @Nullable
    public static Double flopLift(@Nullable EntityType<?> form) {
        if (form == EntityType.DOLPHIN) return 0.5;
        return form != null && FLOP_SOUNDS.containsKey(form) ? 0.4 : null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (!FormTraits.isWaterForm(form)) return;
        if (form == EntityType.DOLPHIN) breatheAir(player);
        else breatheWater(player, form == EntityType.AXOLOTL);
        flopSound(player, form);
    }

    /** {@code WaterAnimal}/{@code Axolotl#handleAirSupply}: full in water, running down out of it. */
    private static void breatheWater(ServerPlayer player, boolean axolotl) {
        int max = axolotl ? AXOLOTL_AIR : FISH_AIR;
        boolean wet = axolotl ? player.isInWaterRainOrBubble() : player.isInWaterOrBubble();
        int air = wet ? max : AIR.getOrDefault(player.getUUID(), max) - 1;
        if (air <= -GASP) {
            air = 0;
            player.hurt(axolotl ? player.damageSources().dryOut() : player.damageSources().drown(), GASP_DAMAGE);
        }
        AIR.put(player.getUUID(), air);
        show(player, air, max);
    }

    /** A dolphin holds its breath under water and dries out on land. */
    private static void breatheAir(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean under = player.isEyeInFluid(FluidTags.WATER);
        int air = under ? AIR.getOrDefault(id, DOLPHIN_AIR) - 1 : DOLPHIN_AIR; // Dolphin#increaseAirSupply: a breath fills it
        if (air <= -GASP) {
            air = 0;
            player.hurt(player.damageSources().drown(), GASP_DAMAGE);
        }
        AIR.put(id, air);
        int moisture = player.isInWaterRainOrBubble() ? DOLPHIN_MOISTURE : MOISTURE.getOrDefault(id, DOLPHIN_MOISTURE) - 1;
        if (moisture <= 0) {
            moisture = 0;
            player.hurt(player.damageSources().dryOut(), DRY_DOLPHIN_DAMAGE);
        }
        MOISTURE.put(id, moisture);
        if (under) show(player, air, DOLPHIN_AIR);
        else show(player, moisture, DOLPHIN_MOISTURE);
    }

    /** The air bar, as a share of this form's own limit. */
    private static void show(ServerPlayer player, int left, int max) {
        player.setAirSupply(Mth.clamp(player.getMaxAirSupply() * left / max, 0, player.getMaxAirSupply()));
    }

    /** The slap of a flop each time a fish form lands out of water (the hop itself is the client's). */
    private static void flopSound(ServerPlayer player, EntityType<?> form) {
        SoundEvent flop = FLOP_SOUNDS.get(form);
        if (flop == null || player.isInWater() || !player.onGround() || !FormTimers.ready(player, "flop", 8)) return;
        player.level().playSound(null, player.blockPosition(), flop, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** From {@link Morphs#setForm}: a new form starts with a full breath. */
    static void onFormChanged(ServerPlayer player) {
        AIR.remove(player.getUUID());
        MOISTURE.remove(player.getUUID());
    }

    /** Test seam: ticks of air the form has left. */
    public static int airLeft(Player player) {
        return AIR.getOrDefault(player.getUUID(), Integer.MAX_VALUE);
    }
}
