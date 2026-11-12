package games.brennan.balancedtotems.registry;

import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-player state, kept in the player's Forge persistent data under {@code balancedtotems}.
 * Stands in for NeoForge's data attachments on 1.20.1: saved with the player, not kept through death.
 */
public abstract class PlayerData<T> {

    private final String key;
    private final T empty;

    private PlayerData(String key, T empty) {
        this.key = key;
        this.empty = empty;
    }

    public static PlayerData<String> string(String key) {
        return new PlayerData<>(key, "") {
            @Override protected String read(CompoundTag tag) { return tag.getString(key()); }
            @Override protected void write(CompoundTag tag, String value) { tag.putString(key(), value); }
        };
    }

    public static PlayerData<Integer> integer(String key) {
        return new PlayerData<>(key, 0) {
            @Override protected Integer read(CompoundTag tag) { return tag.getInt(key()); }
            @Override protected void write(CompoundTag tag, Integer value) { tag.putInt(key(), value); }
        };
    }

    public static PlayerData<List<String>> strings(String key) {
        return new PlayerData<>(key, List.of()) {
            @Override protected List<String> read(CompoundTag tag) {
                List<String> values = new ArrayList<>();
                for (Tag entry : tag.getList(key(), Tag.TAG_STRING)) values.add(entry.getAsString());
                return List.copyOf(values);
            }
            @Override protected void write(CompoundTag tag, List<String> value) {
                ListTag list = new ListTag();
                value.forEach(v -> list.add(StringTag.valueOf(v)));
                tag.put(key(), list);
            }
        };
    }

    protected abstract T read(CompoundTag tag);

    protected abstract void write(CompoundTag tag, T value);

    protected final String key() {
        return key;
    }

    public final boolean has(Player player) {
        return root(player).contains(key);
    }

    public final T get(Player player) {
        CompoundTag root = root(player);
        return root.contains(key) ? read(root) : empty;
    }

    public final void set(Player player, T value) {
        CompoundTag data = player.getPersistentData();
        CompoundTag root = data.getCompound(BalancedTotems.MOD_ID);
        write(root, value);
        data.put(BalancedTotems.MOD_ID, root);
    }

    private static CompoundTag root(Player player) {
        return player.getPersistentData().getCompound(BalancedTotems.MOD_ID);
    }
}
