package games.brennan.balancedtotems.morph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * One morph form's place in the pick: which pool it is drawn from and how often. Lives in the
 * {@code balancedtotems:morph_forms} data map on entity types, so a datapack can change it; the
 * shipped values come from Brennan's weight table. A weight of 0 leaves the form out.
 */
public record MorphForm(Pool pool, int weight) {

    /** Where you died picks the pool: in lava, underwater (eyes in water), or anywhere else. */
    public enum Pool implements StringRepresentable {
        LAND, WATER, LAVA;

        public static final Codec<Pool> CODEC = StringRepresentable.fromEnum(Pool::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<MorphForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Pool.CODEC.fieldOf("pool").forGetter(MorphForm::pool),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("weight").forGetter(MorphForm::weight)
    ).apply(instance, MorphForm::new));

    public static final DataMapType<EntityType<?>, MorphForm> DATA_MAP =
        DataMapType.builder(BalancedTotems.id("morph_forms"), Registries.ENTITY_TYPE, CODEC).build();

    /** Mod-bus listener, added in {@link BalancedTotems}. */
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
