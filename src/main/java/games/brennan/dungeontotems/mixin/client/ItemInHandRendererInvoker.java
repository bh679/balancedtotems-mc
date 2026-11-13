package games.brennan.dungeontotems.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's empty-hand arm pose and swing, reused to hold an armed form's own arm. */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {

    @Invoker("renderPlayerArm")
    void dungeontotems$renderPlayerArm(PoseStack pose, MultiBufferSource buffers, int light, float equippedProgress,
                                       float swingProgress, HumanoidArm side);
}
