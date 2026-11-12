package games.brennan.balancedtotems.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import games.brennan.balancedtotems.BalancedTotems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Swaps each Totem of Undying in a loot drop for a random item from {@link #EVOKER_TOTEMS}, keeping
 * the count. {@code data/balancedtotems/loot_modifiers/evoker_totems.json} limits it to
 * {@code minecraft:entities/evoker}, so the evoker's emeralds are untouched. An empty tag leaves the
 * vanilla totem in place.
 */
public final class EvokerTotemLootModifier extends LootModifier {

    public static final TagKey<Item> EVOKER_TOTEMS =
        TagKey.create(Registries.ITEM, BalancedTotems.id("evoker_totems"));

    public static final Codec<EvokerTotemLootModifier> CODEC = RecordCodecBuilder.create(
        inst -> codecStart(inst).apply(inst, EvokerTotemLootModifier::new));

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BalancedTotems.MOD_ID);

    public static final Supplier<Codec<EvokerTotemLootModifier>> SERIALIZER =
        SERIALIZERS.register("evoker_totems", () -> CODEC);

    public EvokerTotemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Optional<HolderSet.Named<Item>> pool = BuiltInRegistries.ITEM.getTag(EVOKER_TOTEMS);
        if (pool.isEmpty() || pool.get().size() == 0) {
            return generatedLoot;
        }
        ObjectArrayList<ItemStack> swapped = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                Holder<Item> pick = pool.get().getRandomElement(context.getRandom()).orElseThrow();
                swapped.add(new ItemStack(pick.value(), stack.getCount()));
            } else {
                swapped.add(stack);
            }
        }
        return swapped;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
