package games.brennan.dungeontotems.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * A trigger that fires with one key (a curse, a form) and matches criteria naming that key, or any key
 * when the criterion names none:
 * <pre>{@code { "trigger": "dungeontotems:curse_survived", "conditions": { "curse": "weakness" } }}</pre>
 */
public final class KeyTrigger extends SimpleCriterionTrigger<KeyTrigger.Instance> {

    private final Codec<Instance> codec;

    public KeyTrigger(String field) {
        this.codec = RecordCodecBuilder.create(in -> in.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
            Codec.STRING.optionalFieldOf(field).forGetter(Instance::key)
        ).apply(in, Instance::new));
    }

    @Override
    public Codec<Instance> codec() {
        return codec;
    }

    public void trigger(ServerPlayer player, String key) {
        trigger(player, instance -> instance.matches(key));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<String> key) implements SimpleCriterionTrigger.SimpleInstance {
        boolean matches(String fired) {
            return key.map(fired::equals).orElse(true);
        }
    }
}
