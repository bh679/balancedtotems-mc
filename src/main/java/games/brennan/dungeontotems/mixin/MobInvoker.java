package games.brennan.dungeontotems.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

/** A mob's own voice ({@code Mob#getAmbientSound} is protected), for a form to make its sound. */
@Mixin(Mob.class)
public interface MobInvoker {

    @Invoker("getAmbientSound")
    @Nullable
    SoundEvent dungeontotems$ambientSound();
}
