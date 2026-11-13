package games.brennan.dungeontotems.mixin.client;

import games.brennan.dungeontotems.morph.Morphs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A morphed player has no hands in first person. {@code MorphRenderer} cancels NeoForge's hand event, but
 * mods that animate first-person arms (and shader packs' hand pass) can draw them without it, so the whole
 * first-person hand pass is skipped here, ahead of other mods' injects.
 */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void dungeontotems$noHandsInForm(float partialTicks, PoseStack pose, MultiBufferSource.BufferSource buffers,
                                             LocalPlayer player, int light, CallbackInfo ci) {
        if (Morphs.formOf(player).isPresent()) ci.cancel();
    }
}
