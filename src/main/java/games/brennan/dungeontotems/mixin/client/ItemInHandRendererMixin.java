package games.brennan.dungeontotems.mixin.client;

import games.brennan.dungeontotems.client.FormArms;
import games.brennan.dungeontotems.morph.Morphs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A morphed player has no player hands in first person. {@code MorphRenderer} cancels NeoForge's hand event,
 * but mods that animate first-person arms (and shader packs' hand pass) draw them without it, so the whole
 * first-person hand pass is skipped here, ahead of other mods' injects. A form with arms holds nothing but
 * shows its own arm instead ({@link FormArms}): vanilla's empty-hand arm, with its sway and swing, which
 * {@code MorphRenderer} draws as the form's.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class ItemInHandRendererMixin {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void dungeontotems$onlyTheFormsArm(float partialTicks, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                               LocalPlayer player, int light, CallbackInfo ci) {
        if (Morphs.formOf(player).isEmpty()) return;
        ci.cancel();
        if (!FormArms.hasArms(player) || player.isInvisible() || player.isScoping()) return;
        float swing = (player.swingingArm == null || player.swingingArm == InteractionHand.MAIN_HAND) ? player.getAttackAnim(partialTicks) : 0.0F;
        float xBob = Mth.lerp(partialTicks, player.xBobO, player.xBob);
        float yBob = Mth.lerp(partialTicks, player.yBobO, player.yBob);
        float equipped = 1.0F - Mth.lerp(partialTicks, oMainHandHeight, mainHandHeight);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTicks) - xBob) * 0.1F));
        pose.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTicks) - yBob) * 0.1F));
        ((ItemInHandRendererInvoker) this).dungeontotems$renderPlayerArm(pose, buffers, light, equipped, swing, player.getMainArm());
        pose.popPose();
        buffers.endBatch();
    }
}
