package games.brennan.dungeontotems.morph;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * A land form moves through the air at its own pace. A player's air speed ({@code Player#getFlyingSpeed},
 * 0.02) and a sprint-jump's forward push (0.2, {@code LivingEntity#jumpFromGround}) ignore their ground
 * speed, so a turtle would leap ahead far faster than it walks. Both are scaled like the form's ground
 * speed against a player's: a turtle hops at turtle pace, a horse carries its gallop into a jump.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class FormAir {

    /** {@code Player#getFlyingSpeed} off the ground, and a player's own walk it goes with. */
    private static final float PLAYER_AIR_SPEED = 0.02F;
    /** {@code LivingEntity#jumpFromGround}: a sprint-jump's push forward. */
    private static final double SPRINT_JUMP_PUSH = 0.2;

    private FormAir() {}

    /** How fast the form moves against a player at the same pace (1 = a player), or 1 when it isn't a land form. */
    public static double paceOverPlayer(Player player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (MobGaits.of(form) == null || MorphFlight.flies(form) || player.getAbilities().flying) return 1.0;
        double sprint = player.isSprinting() ? MobGaits.PLAYER_SPRINT : 1.0;
        return player.getAttributeValue(Attributes.MOVEMENT_SPEED) / (MobGaits.PLAYER_WALK_SPEED * sprint);
    }

    /** From {@code mixin/PlayerMixin}: the form's air speed (the sprint's +30% is in its speed already). */
    public static float airSpeed(Player player, float vanilla) {
        double pace = paceOverPlayer(player);
        return pace == 1.0 ? vanilla : (float) (vanilla * pace);
    }

    /** A sprint-jump pushes the form forward only as far as its own pace. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isSprinting()) return;
        double pace = paceOverPlayer(player);
        if (pace == 1.0) return;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double take = SPRINT_JUMP_PUSH * (1.0 - pace);
        player.addDeltaMovement(new Vec3(Mth.sin(yaw) * take, 0.0, -Mth.cos(yaw) * take));
    }
}
