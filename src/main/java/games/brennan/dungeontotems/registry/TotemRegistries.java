package games.brennan.dungeontotems.registry;

import com.mojang.serialization.Codec;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.TotemItem;
import games.brennan.dungeontotems.effect.CurseEffect;
import games.brennan.dungeontotems.effect.HeartbreakEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Every registry entry, tag and key the mod owns. */
public final class TotemRegistries {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DungeonTotems.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, DungeonTotems.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DungeonTotems.MOD_ID);

    /** Three fewer hearts per level, never below half a heart (see {@link HeartbreakEffect}). */
    public static final DeferredHolder<MobEffect, MobEffect> HEARTBREAK = EFFECTS.register("heartbreak",
        () -> new HeartbreakEffect(0xB0213A));

    /** You are a random animal while it lasts (see {@code morph/}). Milk does not cure it. */
    public static final DeferredHolder<MobEffect, MobEffect> MORPHED = EFFECTS.register("morphed",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x7A5CC8, false));

    /**
     * You are a zombie until you eat a golden apple while weakened, or die when it
     * runs out. Milk does not cure it. Other mods find it by id ({@code dungeontotems:zombified}).
     */
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED = EFFECTS.register("zombified",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x3E6B2F, false));

    /** Hit something within the timer, or take damage until you do (see {@code curse/CurseTimers}). */
    public static final DeferredHolder<MobEffect, MobEffect> RAGE = EFFECTS.register("rage",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x8B0000, false));

    /** Give a gift within the timer, or take damage until you do (see {@code curse/CurseTimers}). */
    public static final DeferredHolder<MobEffect, MobEffect> LONELY = EFFECTS.register("lonely",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0xE68C9C, false));

    /** Hurt anything and it costs you (see {@code curse/VowOfPeace}). Milk does not lift it. */
    public static final DeferredHolder<MobEffect, MobEffect> VOW_OF_PEACE = EFFECTS.register("vow_of_peace",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0xE4E4EC, false));

    /** Eat anything but melon and it costs you (see {@code curse/RuleCurses}). */
    public static final DeferredHolder<MobEffect, MobEffect> MELON_DIET = EFFECTS.register("melon_diet",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x62A236, false));

    /** Open a chest, barrel or ender chest and it costs you (see {@code curse/RuleCurses}). */
    public static final DeferredHolder<MobEffect, MobEffect> RESTRAINED = EFFECTS.register("restrained",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x8A6A3A, false));

    /** You lose air even out of water, and drown on land (see {@code curse/Breathless}). */
    public static final DeferredHolder<MobEffect, MobEffect> BREATHLESS = EFFECTS.register("breathless",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x56B8FF, false));

    /** Phantoms come for you, by day as well as night (see {@code curse/InsomniaPhantoms}). */
    public static final DeferredHolder<MobEffect, MobEffect> INSOMNIA = EFFECTS.register("insomnia",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x2C3664, false));

    /** The entity type a player is shown and played as, by id; empty for their own body. */
    public static final Supplier<AttachmentType<String>> FORM = ATTACHMENTS.register("form",
        () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /** Game time each curse's after-save protection ends (by Curse name); see {@code curse/SavedCurseShield}. */
    public static final Supplier<AttachmentType<Map<String, Long>>> SAVED_UNTIL = ATTACHMENTS.register("saved_until",
        () -> AttachmentType.<Map<String, Long>>builder(() -> Map.of())
            .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG)).build());

    /** Game time of your last hit, for Rage. */
    public static final Supplier<AttachmentType<Long>> LAST_HIT = ATTACHMENTS.register("last_hit",
        () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    /** Game time of your last gift, for Lonely. */
    public static final Supplier<AttachmentType<Long>> LAST_GIFT = ATTACHMENTS.register("last_gift",
        () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final Map<Curse, DeferredItem<TotemItem>> TOTEMS = registerTotems();

    /** Forms that fly: always airborne, no walking or jumping (see {@code morph/MorphFlight}). */
    public static final TagKey<EntityType<?>> FLYING_FORMS =
        TagKey.create(Registries.ENTITY_TYPE, DungeonTotems.id("flying_forms"));

    public static final ResourceKey<DamageType> ZOMBIFICATION =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("zombification"));
    public static final ResourceKey<DamageType> RAGE_FAILED =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("rage_failed"));
    public static final ResourceKey<DamageType> LONELINESS =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("loneliness"));
    public static final ResourceKey<DamageType> BROKEN_VOW =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("broken_vow"));
    public static final ResourceKey<DamageType> BROKEN_DIET =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("broken_diet"));
    public static final ResourceKey<DamageType> BROKEN_RESTRAINT =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("broken_restraint"));
    public static final ResourceKey<DamageType> SUFFOCATION =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("breathless"));
    /** A bee-form player who stung and didn't turn back in time. */
    public static final ResourceKey<DamageType> LOST_STINGER =
        ResourceKey.create(Registries.DAMAGE_TYPE, DungeonTotems.id("lost_stinger"));

    private TotemRegistries() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(TotemRegistries::addToCreativeTab);
    }

    private static Map<Curse, DeferredItem<TotemItem>> registerTotems() {
        Map<Curse, DeferredItem<TotemItem>> totems = new EnumMap<>(Curse.class);
        for (Curse curse : Curse.values()) {
            totems.put(curse, ITEMS.register(curse.itemName(),
                () -> new TotemItem(curse, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))));
        }
        return Map.copyOf(totems);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;
        for (Curse curse : Curse.values()) {
            event.accept(TOTEMS.get(curse));
        }
    }
}
