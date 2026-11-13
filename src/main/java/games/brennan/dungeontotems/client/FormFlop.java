package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.morph.WaterLife;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A fish or dolphin out of water flops ({@code AbstractFish#aiStep}, {@code Dolphin#tick}): each time it
 * lands it bounces up and a little to one side. Jumping flops you toward where you look. A rabbit has no
 * walk: it gets about by hopping ({@code Rabbit#getJumpPower}), so a moving rabbit form hops each time it
 * lands, and its stand-in plays the hop. The client moves its own player, so this runs there.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID, value = Dist.CLIENT)
public final class FormFlop {

    /** {@code AbstractFish}: up to 0.05 sideways each flop; a dolphin's are wilder (0.1). */
    private static final double FISH_SWAY = 0.05;
    private static final double DOLPHIN_SWAY = 0.1;
    /** A jump's flop toward where you look. */
    private static final double AIMED_FLOP = 0.25;
    /** {@code Rabbit#getJumpPower}: a rabbit's hop on the move. */
    private static final double RABBIT_HOP = 0.3;

    private FormFlop() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || player != Minecraft.getInstance().player) return;
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == EntityType.RABBIT) {
            hop(player);
            return;
        }
        Double lift = WaterLife.flopLift(form);
        if (lift == null || player.isInWater() || !player.onGround()) return;
        RandomSource random = player.getRandom();
        Vec3 flop;
        if (player.input.jumping) {
            Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            flop = look.scale(AIMED_FLOP);
        } else {
            double sway = form == EntityType.DOLPHIN ? DOLPHIN_SWAY : FISH_SWAY;
            flop = new Vec3((random.nextFloat() * 2.0F - 1.0F) * sway, 0.0, (random.nextFloat() * 2.0F - 1.0F) * sway);
        }
        player.setDeltaMovement(player.getDeltaMovement().add(flop.x, lift, flop.z));
        player.setOnGround(false);
    }

    /** A rabbit on the move hops as it lands; a jump is still its own, higher leap. */
    private static void hop(LocalPlayer player) {
        boolean moving = player.input.getMoveVector().lengthSquared() > 0.0F;
        if (!moving || player.input.jumping || !player.onGround() || player.isInWater()) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, RABBIT_HOP, motion.z);
        player.setOnGround(false);
    }
}
