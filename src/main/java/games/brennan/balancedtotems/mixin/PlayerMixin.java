package games.brennan.balancedtotems.mixin;

import games.brennan.balancedtotems.morph.FormAir;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A land form moves through the air at its own pace ({@link FormAir}). */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void balancedtotems$airSpeedOfTheForm(CallbackInfoReturnable<Float> result) {
        result.setReturnValue(FormAir.airSpeed((Player) (Object) this, result.getReturnValue()));
    }
}
