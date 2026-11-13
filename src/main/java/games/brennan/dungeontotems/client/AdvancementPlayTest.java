package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.advancement.TabComplete;
import games.brennan.dungeontotems.advancement.TotemTriggers;
import games.brennan.dungeontotems.curse.Curses;
import games.brennan.dungeontotems.morph.MorphForm;
import games.brennan.dungeontotems.morph.Morphs;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Development only ({@code ./gradlew runSelfTest -Dadvancements=play}): plays the whole Dungeon Totems tab
 * the way a player would, on the integrated server. It pops each totem by dying with it held, lives through
 * each curse's after-save time (cut short), cures Zombified with a golden apple, and falls into the void,
 * off a height, onto lit TNT and burns. Then it
 * records which advancements it earned. {@link AdvancementSelfTest} screenshots the tab afterwards.
 */
final class AdvancementPlayTest {

    /** Ticks between steps, long enough for a pop's animation and a shortened curse to run out. */
    static final int STEP_TICKS = 60;
    /** A curse's timer, shortened so the run doesn't take 20 minutes. */
    private static final int SHORT = 20;
    private static final float LETHAL = 1000.0F;

    private record Step(String name, Consumer<ServerPlayer> action) {}

    /** Curses lived through after a save: their after-save time cut short so the run doesn't take minutes. */
    private static final List<Curse> LIVED_THROUGH = List.of(Curse.WEAKNESS, Curse.FATIGUE, Curse.HEARTBREAK, Curse.HUNGER,
        Curse.SLOWNESS, Curse.BREATHLESSNESS, Curse.INSOMNIA, Curse.BLINDNESS, Curse.MORPHING,
        Curse.RAGE, Curse.LONELINESS, Curse.PACIFISM, Curse.MELON_DIET, Curse.RESTRAINT);

    private static final List<Step> STEPS = List.of(
        new Step("arrive (spawn protection wears off)", player -> {}),
        new Step("every curse, saved and lived through", player -> {
            for (Curse curse : LIVED_THROUGH) popByDying(player, curse);
            for (Curse curse : LIVED_THROUGH) shorten(player, curse);
        }),
        new Step("zombified, cured with a golden apple", player -> {
            popByDying(player, Curse.ZOMBIFIED);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
            EventHooks.onItemUseFinish(player, new ItemStack(Items.GOLDEN_APPLE), 0, ItemStack.EMPTY);
        }),
        new Step("void", player -> {
            hold(player, Curse.VOID);
            player.teleportTo(player.getX(), player.level().getMinBuildHeight() - 70, player.getZ());
        }),
        new Step("feather", player -> {
            hold(player, Curse.FEATHER);
            player.setHealth(4.0F);
            player.teleportTo(player.getX(), player.getY() + 60, player.getZ());
        }),
        new Step("boom", player -> {
            hold(player, Curse.BOOM);
            player.setHealth(2.0F);
            PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), null);
            tnt.setFuse(0);
            player.level().addFreshEntity(tnt);
        }),
        new Step("fire", player -> {
            floor(player.server); // the TNT blew a hole in it, and dropped you through
            player.resetFallDistance();
            hold(player, Curse.FIRE);
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
            player.removeEffect(MobEffects.REGENERATION); // earlier saves' healing would outlast a slow burn
            player.removeEffect(MobEffects.ABSORPTION);
            player.setHealth(1.0F);
            player.level().setBlockAndUpdate(player.blockPosition(), Blocks.LAVA.defaultBlockState());
        }),
        new Step("morphing, five real morphs", player -> {
            floor(player.server); // out of the lava

            for (int i = 0; i < 5; i++) popByDying(player, Curse.MORPHING);
        }),
        new Step("the rest of the animals (not playable here)", player -> {
            BuiltInRegistries.ENTITY_TYPE.getDataMap(MorphForm.DATA_MAP).keySet().forEach(key ->
                TotemTriggers.BECAME_FORM.get().trigger(player, key.location().toString()));
        }),
        new Step("settle", player -> {}));

    private static final List<String> REPORT = new ArrayList<>();
    private static final Set<String> REAL_FORMS = new HashSet<>();

    private AdvancementPlayTest() {}

    static int ticks() {
        return STEPS.size() * STEP_TICKS;
    }

    /** Runs step {@code t / STEP_TICKS} on its first tick. */
    static void tick(MinecraftServer server, ServerPlayer player, int t) {
        if (t == 0) setUp(server);
        if (t % STEP_TICKS != 0 || t / STEP_TICKS >= STEPS.size()) return;
        Step step = STEPS.get(t / STEP_TICKS);
        server.execute(() -> {
            float before = player.getHealth();
            step.action().accept(player);
            REPORT.add(String.format("step %-40s alive %s, health %.0f -> %.0f, form %s, at %s", step.name(), player.isAlive(),
                before, player.getHealth(), Morphs.formOf(player).map(f -> BuiltInRegistries.ENTITY_TYPE.getKey(f).getPath()).orElse("player"),
                player.blockPosition().toShortString()));
            Morphs.formOf(player).ifPresent(form -> REAL_FORMS.add(BuiltInRegistries.ENTITY_TYPE.getKey(form).toString()));
        });
    }

    private static void setUp(MinecraftServer server) {
        floor(server);
        run(server, "advancement revoke @a from dungeontotems:terms_and_conditions");
        run(server, "effect clear @a");
        run(server, "effect give @a minecraft:instant_health 1 10 true");
        run(server, "clear @a");
        run(server, "gamemode survival @a");
        run(server, "difficulty normal");
        run(server, "gamerule doMobSpawning false");
        run(server, "kill @e[type=!player]");
    }

    /** A clean stone floor away from the morph self-test's lava pool, where a void rescue lands safely. */
    private static void floor(MinecraftServer server) {
        run(server, "fill 192 149 192 208 149 208 minecraft:stone");
        run(server, "fill 192 150 192 208 160 208 minecraft:air");
        run(server, "tp @a 200 150 200");
    }

    /** The tab as it stands: each advancement earned or not, with its progress. */
    static List<String> results(MinecraftServer server, ServerPlayer player) {
        List<String> out = new ArrayList<>(REPORT);
        List<ResourceLocation> tab = new ArrayList<>(TabComplete.TAB);
        tab.add(TabComplete.FULFILLED);
        for (ResourceLocation id : tab) {
            AdvancementHolder holder = server.getAdvancements().get(id);
            if (holder == null) {
                out.add(id.getPath() + ": NOT LOADED");
                continue;
            }
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            Component text = progress.getProgressText();
            List<String> missing = new ArrayList<>();
            progress.getRemainingCriteria().forEach(missing::add);
            out.add(String.format("%-32s %s", id.getPath(), progress.isDone() ? "EARNED"
                : "not earned" + (text == null ? "" : " " + text.getString()) + (missing.size() <= 10 ? ", missing " + missing : "")));
        }
        out.add("animals morphed into for real: " + REAL_FORMS);
        out.add("curse trials left open: " + player.getData(TotemRegistries.CURSE_TRIALS));
        return out;
    }

    private static void popByDying(ServerPlayer player, Curse curse) {
        hold(player, curse);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), LETHAL);
        Morphs.formOf(player).ifPresent(form -> REAL_FORMS.add(BuiltInRegistries.ENTITY_TYPE.getKey(form).toString()));
    }

    private static void hold(ServerPlayer player, Curse curse) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TotemRegistries.TOTEMS.get(curse).get()));
    }

    /** A save, shorter: the curse and its after-save protection both end in {@link #SHORT} ticks, as {@code Curses.pop} sets them. */
    private static void shorten(ServerPlayer player, Curse curse) {
        MobEffectInstance current = player.getEffect(curse.effect());
        if (current != null) Curses.apply(player, curse, current.getAmplifier(), SHORT);
        Curses.markSaved(player, curse, SHORT);
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
