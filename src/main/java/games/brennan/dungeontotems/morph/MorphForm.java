package games.brennan.dungeontotems.morph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * One morph form's place in the pick: which pool it is drawn from, how often, and how many seconds
 * the morph lasts once it lands on this form. Lives in the {@code dungeontotems:morph_forms} data map
 * on entity types, so a datapack can change it; the shipped values come from Brennan's table. A weight
 * of 0 leaves the form out; a form without {@code seconds} lasts the curse's usual 30.
 */
public record MorphForm(Pool pool, int weight, int seconds) {

    /** {@code Curse.MORPHING}'s after-save time, for forms that don't set their own. */
    public static final int DEFAULT_SECONDS = 30;
    /** Longest a form may set: an hour. */
    public static final int MAX_SECONDS = 3600;

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
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("weight").forGetter(MorphForm::weight),
        Codec.intRange(1, MAX_SECONDS).optionalFieldOf("seconds", DEFAULT_SECONDS).forGetter(MorphForm::seconds)
    ).apply(instance, MorphForm::new));

    /** How long a morph into this form lasts, in ticks. */
    public int ticks() {
        return seconds * 20;
    }

    public static final DataMapType<EntityType<?>, MorphForm> DATA_MAP =
        DataMapType.builder(DungeonTotems.id("morph_forms"), Registries.ENTITY_TYPE, CODEC).build();

    /** Mod-bus listener, added in {@link DungeonTotems}. */
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
