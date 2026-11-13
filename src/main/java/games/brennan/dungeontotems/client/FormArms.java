package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.mixin.client.ModelPartAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A form with arms (a zombified piglin, the golems, an allay) shows its own arm in first person where a
 * player's would be. The arm is found in the form's model by part, so any armed form works.
 *
 * <p>The arm hangs straight down with the top centre of its box where a player's arm's is, at true size,
 * except one shorter than a player's arm is stretched to that length so it reads on screen.</p>
 */
public final class FormArms {

    /** A player arm's box top centre, in model pixels from the arm's pose: right arm (-5, 2, 0) + box centre. */
    private static final float PLAYER_ARM_X = 6.0F;
    private static final float PLAYER_ARM_TOP = 0.0F;
    private static final float PLAYER_ARM_LENGTH = 12.0F;
    /** Names hierarchical models give their arms. */
    private static final String RIGHT_ARM = "right_arm";
    private static final String LEFT_ARM = "left_arm";

    private static int drawn;

    private FormArms() {}

    /** Whether the player's form has an arm to show in first person. */
    public static boolean hasArms(Player player) {
        LivingEntity standIn = MorphRenderer.standInFor(player);
        return standIn != null && arm(model(standIn), HumanoidArm.RIGHT) != null;
    }

    /** How often a form's arm has been drawn ({@link FormSelfTest} expects it for armed forms). */
    static int drawn() {
        return drawn;
    }

    /** Draws the form's arm on {@code side}, posed as vanilla poses a player's empty hand. */
    static void render(Player player, HumanoidArm side, PoseStack pose, MultiBufferSource buffers, int light) {
        LivingEntity standIn = MorphRenderer.standInFor(player);
        if (standIn == null || standIn.isInvisible()) return;
        EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(standIn);
        EntityModel<?> model = model(standIn);
        ModelPart arm = arm(model, side);
        if (arm == null) return;
        ResourceLocation texture = renderer.getTextureLocation(standIn);
        PartPose saved = arm.storePose();
        float xScale = arm.xScale, yScale = arm.yScale, zScale = arm.zScale;
        try {
            hangDown(arm, side);
            arm.render(pose, buffers.getBuffer(model.renderType(texture)), light, OverlayTexture.NO_OVERLAY);
            drawn++;
        } finally {
            arm.loadPose(saved);
            arm.xScale = xScale;
            arm.yScale = yScale;
            arm.zScale = zScale;
        }
    }

    @Nullable
    private static EntityModel<?> model(LivingEntity standIn) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(standIn);
        return renderer instanceof LivingEntityRenderer<?, ?> living ? living.getModel() : null;
    }

    @Nullable
    private static ModelPart arm(@Nullable EntityModel<?> model, HumanoidArm side) {
        if (model instanceof HumanoidModel<?> humanoid) return side == HumanoidArm.RIGHT ? humanoid.rightArm : humanoid.leftArm;
        if (model instanceof HierarchicalModel<?> hierarchical) return find(hierarchical.root(), side == HumanoidArm.RIGHT ? RIGHT_ARM : LEFT_ARM);
        return null;
    }

    @Nullable
    private static ModelPart find(ModelPart part, String name) {
        for (var child : ((ModelPartAccessor) (Object) part).dungeontotems$children().entrySet()) {
            if (child.getKey().equals(name)) return child.getValue();
            ModelPart found = find(child.getValue(), name);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Turns the arm so its long side points down, stretches it to a player's arm length if shorter, and moves
     * its pivot so the box's top centre sits where a player's arm's does.
     */
    private static void hangDown(ModelPart arm, HumanoidArm side) {
        float[] box = bounds(arm, new Quaternionf());
        boolean sideways = box[3] - box[0] > box[4] - box[1]; // a snow golem's stick lies along x
        Quaternionf rotation = sideways ? new Quaternionf().rotationZ(Mth.HALF_PI) : new Quaternionf();
        arm.setRotation(0.0F, 0.0F, sideways ? Mth.HALF_PI : 0.0F);
        float[] turned = bounds(arm, rotation);
        float length = turned[4] - turned[1];
        float scale = length > 0.0F && length < PLAYER_ARM_LENGTH ? PLAYER_ARM_LENGTH / length : 1.0F;
        arm.xScale = scale;
        arm.yScale = scale;
        arm.zScale = scale;
        float sideX = side == HumanoidArm.RIGHT ? -PLAYER_ARM_X : PLAYER_ARM_X;
        arm.setPos(
            sideX - scale * (turned[0] + turned[3]) / 2.0F,
            PLAYER_ARM_TOP - scale * turned[1],
            -scale * (turned[2] + turned[5]) / 2.0F);
    }

    /** The part's boxes' bounds after {@code rotation}: min x, y, z, then max x, y, z, in model pixels. */
    private static float[] bounds(ModelPart arm, Quaternionf rotation) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) arm).dungeontotems$cubes()) {
            for (int corner = 0; corner < 8; corner++) {
                Vector3f point = rotation.transform(new Vector3f(
                    (corner & 1) == 0 ? cube.minX : cube.maxX,
                    (corner & 2) == 0 ? cube.minY : cube.maxY,
                    (corner & 4) == 0 ? cube.minZ : cube.maxZ));
                box[0] = Math.min(box[0], point.x);
                box[1] = Math.min(box[1], point.y);
                box[2] = Math.min(box[2], point.z);
                box[3] = Math.max(box[3], point.x);
                box[4] = Math.max(box[4], point.y);
                box[5] = Math.max(box[5], point.z);
            }
        }
        return box;
    }
}
