package games.brennan.dungeontotems.mixin;

import games.brennan.dungeontotems.morph.Morphs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player in a fire-immune form (strider, zombified piglin) is fire-immune as the mob is: lava and fire
 * don't set them alight, so they don't burn or show flames.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void dungeontotems$fireImmuneAsTheForm(CallbackInfoReturnable<Boolean> result) {
        if ((Object) this instanceof Player player && Morphs.formOf(player).map(EntityType::fireImmune).orElse(false)) {
            result.setReturnValue(true);
        }
    }
}
