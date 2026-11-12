package games.brennan.balancedtotems.morph;

import games.brennan.balancedtotems.mixin.MobInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A form's own sound, when a click has nothing else to do: a cat meows, a pig oinks. Uses the mob's ambient
 * sound and voice pitch; mobs with no ambient sound use another of theirs.
 */
public final class FormSounds {

    /** Ticks between sounds, so holding a button doesn't make a din. */
    private static final int COOLDOWN = 10;
    private static final Map<EntityType<?>, SoundEvent> NO_AMBIENT = Map.of(
        EntityType.IRON_GOLEM, SoundEvents.IRON_GOLEM_HURT,
        EntityType.TADPOLE, SoundEvents.TADPOLE_FLOP);

    private FormSounds() {}

    /** Plays the player's form's sound, if it's been long enough since the last. */
    public static void speak(ServerPlayer player) {
        EntityType<?> form = Morphs.formOf(player).orElse(null);
        if (form == null || !FormTimers.ready(player, "speak", COOLDOWN)) return;
        if (!(form.create(player.level()) instanceof Mob mob)) return;
        SoundEvent sound = soundOf(form, mob);
        if (sound == null) return;
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, mob.getVoicePitch());
    }

    @Nullable
    private static SoundEvent soundOf(EntityType<?> form, Mob mob) {
        SoundEvent ambient = ((MobInvoker) mob).balancedtotems$ambientSound();
        return ambient != null ? ambient : NO_AMBIENT.get(form);
    }
}
