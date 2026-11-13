package games.brennan.dungeontotems.mixin;

import games.brennan.dungeontotems.morph.FormTraits;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player in strider form stands on lava as a strider does ({@code Strider#canStandOnFluid}): lava's
 * surface holds them up and they walk on it with ground movement, not lava's drag.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void dungeontotems$standOnLavaAsAStrider(FluidState fluid, CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof Player player && FormTraits.standsOn(player, fluid)) result.setReturnValue(true);
    }
}
