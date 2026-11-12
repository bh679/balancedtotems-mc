package games.brennan.balancedtotems.curse;

import games.brennan.balancedtotems.Curse;
import games.brennan.balancedtotems.TotemItem;
import games.brennan.balancedtotems.registry.BtRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tells a curse apart from the same effect from a potion (a Weakness curse from Weakness splashed on you).
 *
 * <p>On 1.20.1 an effect instance carries its own curative items, saved with it. A curse's list holds its
 * totem as a mark, next to milk when milk cures it; drinking milk only ever looks for milk. NeoForge 1.21.1
 * does the same job by taking {@code PROTECTED_BY_TOTEM} off the instance's cures.</p>
 */
public final class CurseMarks {

    private CurseMarks() {}

    /** Marks {@code instance} as {@code curse}'s, so another totem's pop leaves it. */
    public static void mark(MobEffectInstance instance, Curse curse) {
        List<ItemStack> cures = new ArrayList<>(instance.getCurativeItems());
        cures.add(new ItemStack(BtRegistries.TOTEMS.get(curse).get()));
        instance.setCurativeItems(cures);
    }

    public static boolean isMarked(MobEffectInstance instance) {
        for (ItemStack cure : instance.getCurativeItems()) {
            if (cure.getItem() instanceof TotemItem) return true;
        }
        return false;
    }

    /** What a vanilla totem clears: every effect except the curses. */
    public static void clearForTotem(ServerPlayer player) {
        for (MobEffectInstance instance : List.copyOf(player.getActiveEffects())) {
            if (!isMarked(instance)) player.removeEffect(instance.getEffect());
        }
    }
}
