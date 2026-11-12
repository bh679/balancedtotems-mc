package games.brennan.balancedtotems.registry;

import com.mojang.serialization.Codec;
import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.TotemItem;
import games.brennan.balancedtotems.effect.CurseEffect;
import games.brennan.balancedtotems.effect.HeartbreakEffect;
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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Every registry entry, tag and key the mod owns. */
public final class BtRegistries {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BalancedTotems.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, BalancedTotems.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BalancedTotems.MOD_ID);

    /** Two fewer hearts per level, never below half a heart (see {@link HeartbreakEffect}). */
    public static final DeferredHolder<MobEffect, MobEffect> HEARTBREAK = EFFECTS.register("heartbreak",
        () -> new HeartbreakEffect(0xB0213A));

    /** You are a random animal while it lasts (see {@code morph/}). Milk does not cure it. */
    public static final DeferredHolder<MobEffect, MobEffect> MORPHED = EFFECTS.register("morphed",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x7A5CC8, false));

    /**
     * You are a zombie until you eat a golden apple while weakened, or die when it
     * runs out. Milk does not cure it. Other mods find it by id ({@code balancedtotems:zombified}).
     */
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED = EFFECTS.register("zombified",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x3E6B2F, false));

    /** Kill {@link #RAMPAGE_KILLS_NEEDED} players or PlayerMobs per level before it runs out, or die. */
    public static final DeferredHolder<MobEffect, MobEffect> RAMPAGE = EFFECTS.register("rampage",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x8B0000, false));

    /** Make {@link #RAMPAGE_KILLS_NEEDED} friends per level before it runs out, or die (see {@code curse/LonelyFriends}). */
    public static final DeferredHolder<MobEffect, MobEffect> LONELY = EFFECTS.register("lonely",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0xE68C9C, false));

    /** Hurt anything while it lasts and you die (see {@code curse/VowOfPeace}). Milk does not lift it. */
    public static final DeferredHolder<MobEffect, MobEffect> VOW_OF_PEACE = EFFECTS.register("vow_of_peace",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0xE4E4EC, false));

    /** Eat anything but melon while it lasts and you die (see {@code curse/RuleCurses}). */
    public static final DeferredHolder<MobEffect, MobEffect> MELON_DIET = EFFECTS.register("melon_diet",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x62A236, false));

    /** Open a chest, barrel or ender chest while it lasts and you die (see {@code curse/RuleCurses}). */
    public static final DeferredHolder<MobEffect, MobEffect> RESTRAINED = EFFECTS.register("restrained",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x8A6A3A, false));

    /** Kills (Rage) or friends (Lonely) needed per level. */
    public static final int RAMPAGE_KILLS_NEEDED = 3;

    /** The entity type a player is shown and played as, by id; empty for their own body. */
    public static final Supplier<AttachmentType<String>> FORM = ATTACHMENTS.register("form",
        () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /** Rampage kills so far in the current rampage. */
    public static final Supplier<AttachmentType<Integer>> RAMPAGE_KILLS = ATTACHMENTS.register("rampage_kills",
        () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Friends made so far while Lonely. */
    public static final Supplier<AttachmentType<Integer>> LONELY_FRIENDS = ATTACHMENTS.register("lonely_friends",
        () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Who those friends are (entity UUIDs), so each one counts once. */
    public static final Supplier<AttachmentType<List<String>>> LONELY_FRIEND_IDS = ATTACHMENTS.register("lonely_friend_ids",
        () -> AttachmentType.<List<String>>builder(() -> List.of()).serialize(Codec.STRING.listOf()).build());

    public static final Map<Curse, DeferredItem<TotemItem>> TOTEMS = registerTotems();

    public static final TagKey<EntityType<?>> MORPH_POOL =
        TagKey.create(Registries.ENTITY_TYPE, BalancedTotems.id("morph_pool"));
    /** What counts as a Rampage kill besides real players (PlayerMob, when it is installed). */
    public static final TagKey<EntityType<?>> RAMPAGE_TARGETS =
        TagKey.create(Registries.ENTITY_TYPE, BalancedTotems.id("rampage_targets"));

    public static final ResourceKey<DamageType> ZOMBIFICATION =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("zombification"));
    public static final ResourceKey<DamageType> RAMPAGE_FAILED =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("rampage_failed"));
    public static final ResourceKey<DamageType> LONELINESS =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("loneliness"));
    public static final ResourceKey<DamageType> BROKEN_VOW =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("broken_vow"));
    public static final ResourceKey<DamageType> BROKEN_DIET =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("broken_diet"));
    public static final ResourceKey<DamageType> BROKEN_RESTRAINT =
        ResourceKey.create(Registries.DAMAGE_TYPE, BalancedTotems.id("broken_restraint"));

    private BtRegistries() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(BtRegistries::addToCreativeTab);
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
