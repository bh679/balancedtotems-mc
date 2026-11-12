package games.brennan.dungeontotems.curse;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Insomnia: phantoms come for you whether you have slept or not. Vanilla's {@code PhantomSpawner} rules,
 * minus its time-since-rest roll and its night-only check: every 60 to 119 seconds, if you can see the sky
 * at or above sea level and the local difficulty allows it, phantoms spawn above you. At night they always
 * do (vanilla's chance at the longest insomnia); by day a quarter of the time, and those day phantoms get
 * Fire Resistance so the sun can't burn them. Your real time since rest is left alone.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID)
public final class InsomniaPhantoms {

    /** Chance a try spawns phantoms at night: vanilla's (T - 72000) / T as T grows without end. */
    public static final float NIGHT_CHANCE = 1.0F;
    /**
     * Chance by day: the lesser of half the night chance (0.5) and "level 1" insomnia, the first night
     * vanilla can spawn phantoms at all, four days awake: (96000 - 72000) / 96000 = 0.25.
     */
    public static final float DAY_CHANCE = 0.25F;

    private static final int MIN_WAIT_TICKS = 60 * 20;
    private static final int EXTRA_WAIT_TICKS = 60 * 20;
    /** Vanilla counts it "night" for phantoms once the sky has darkened this far. */
    private static final int NIGHT_SKY_DARKEN = 5;

    private static final Map<UUID, Integer> NEXT_TRY = new ConcurrentHashMap<>();

    private InsomniaPhantoms() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasEffect(TotemRegistries.INSOMNIA)) return;
        RandomSource random = player.getRandom();
        Integer next = NEXT_TRY.get(player.getUUID());
        if (next == null || next <= 1) {
            // The first try waits a full timer too, like vanilla's spawner after a restart.
            NEXT_TRY.put(player.getUUID(), MIN_WAIT_TICKS + random.nextInt(EXTRA_WAIT_TICKS));
            if (next != null) trySpawn(player, random);
            return;
        }
        NEXT_TRY.put(player.getUUID(), next - 1);
    }

    /** One vanilla-style try at spawning phantoms over {@code player}; returns how many spawned. */
    public static int trySpawn(ServerPlayer player, RandomSource random) {
        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOINSOMNIA)) return 0;
        if (player.isSpectator() || player.isCreative()) return 0;
        BlockPos pos = player.blockPosition();
        boolean skylit = level.dimensionType().hasSkyLight();
        if (skylit && (pos.getY() < level.getSeaLevel() || !level.canSeeSky(pos))) return 0;
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
        if (!difficulty.isHarderThan(random.nextFloat() * 3.0F)) return 0;
        boolean day = skylit && level.getSkyDarken() < NIGHT_SKY_DARKEN;
        if (random.nextFloat() >= (day ? DAY_CHANCE : NIGHT_CHANCE)) return 0;
        BlockPos spawnAt = pos.above(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, spawnAt, level.getBlockState(spawnAt), level.getFluidState(spawnAt), EntityType.PHANTOM)) {
            return 0;
        }
        return spawnGroup(level, spawnAt, difficulty, day, random);
    }

    /** Vanilla's group of 1 to (difficulty + 1) phantoms; by day each gets Fire Resistance for good. */
    public static int spawnGroup(ServerLevel level, BlockPos at, DifficultyInstance difficulty, boolean day, RandomSource random) {
        SpawnGroupData group = null;
        int count = 1 + random.nextInt(difficulty.getDifficulty().getId() + 1);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Phantom phantom = EntityType.PHANTOM.create(level);
            if (phantom == null) continue;
            phantom.moveTo(at, 0.0F, 0.0F);
            group = phantom.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, group);
            if (day) phantom.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
            level.addFreshEntityWithPassengers(phantom);
            spawned++;
        }
        return spawned;
    }
}
