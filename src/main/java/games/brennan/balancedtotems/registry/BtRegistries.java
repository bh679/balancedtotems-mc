package games.brennan.balancedtotems.registry;

import com.mojang.serialization.Codec;
import games.brennan.balancedtotems.BalancedTotems;
import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.TotemItem;
import games.brennan.balancedtotems.effect.CurseEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
public final class BtRegistries {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BalancedTotems.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, BalancedTotems.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BalancedTotems.MOD_ID);

    /** Two fewer hearts: max health -4 while the effect lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> HEARTBREAK = EFFECTS.register("heartbreak",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0xB0213A, true)
            .addAttributeModifier(Attributes.MAX_HEALTH, BalancedTotems.id("effect.heartbreak"),
                -4.0, AttributeModifier.Operation.ADD_VALUE));

    /** You are a random animal while it lasts (see {@code morph/}). Milk does not cure it. */
    public static final DeferredHolder<MobEffect, MobEffect> MORPHED = EFFECTS.register("morphed",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x7A5CC8, false));

    /**
     * You are a zombie until you eat a golden apple or take a potion of weakness, or die when it
     * runs out. Milk does not cure it. Other mods find it by id ({@code balancedtotems:zombified}).
     */
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFIED = EFFECTS.register("zombified",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x3E6B2F, false));

    /** Kill {@link #RAMPAGE_KILLS_NEEDED} players or PlayerMobs before it runs out, or die. */
    public static final DeferredHolder<MobEffect, MobEffect> RAMPAGE = EFFECTS.register("rampage",
        () -> new CurseEffect(MobEffectCategory.HARMFUL, 0x8B0000, false));

    public static final int RAMPAGE_KILLS_NEEDED = 3;

    /** The entity type a player is shown and played as, by id; empty for their own body. */
    public static final Supplier<AttachmentType<String>> FORM = ATTACHMENTS.register("form",
        () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).build());

    /** Rampage kills so far in the current rampage. */
    public static final Supplier<AttachmentType<Integer>> RAMPAGE_KILLS = ATTACHMENTS.register("rampage_kills",
        () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

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
