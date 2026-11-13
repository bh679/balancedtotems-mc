package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.advancement.TabComplete;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Development only ({@code ./gradlew runSelfTest -Dadvancements=true}): grants part of the Dungeon Totems
 * tab ({@code -Dadvancements=play} plays it instead: {@link AdvancementPlayTest}), opens the advancements screen on it and points at each advancement, saving a screenshot of each
 * tooltip ({@code screenshots/selftest_advancement_*}) and the tooltip text to
 * {@code selftest/advancements.txt}. Then the client quits.
 */
@EventBusSubscriber(modid = DungeonTotems.MOD_ID, value = Dist.CLIENT)
public final class AdvancementSelfTest {

    static final boolean ENABLED = !System.getProperty("dungeontotems.selftest.advancements", "").isEmpty();
    private static final boolean PLAY = "play".equals(System.getProperty("dungeontotems.selftest.advancements"));
    private static final int WARM_UP_TICKS = 60;
    private static final int TICKS_PER_SHOT = 10;
    /** {@code AdvancementsScreen}: the window, and where its inside starts. */
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 140;
    private static final int INSIDE_X = 9;
    private static final int INSIDE_Y = 18;

    private static final List<String> REPORT = new ArrayList<>();
    private static int tick;

    private AdvancementSelfTest() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null) return;
        tick++;
        ServerPlayer player = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (player == null) return;
        int play = PLAY ? AdvancementPlayTest.ticks() : 0;
        if (PLAY && tick >= WARM_UP_TICKS && tick < WARM_UP_TICKS + play) {
            AdvancementPlayTest.tick(server, player, tick - WARM_UP_TICKS);
            return;
        }
        int t = tick - play; // the rest runs after the play-through
        List<ResourceLocation> shots = tab();
        if (t == WARM_UP_TICKS && !PLAY) grantSome(server);
        if (t == WARM_UP_TICKS + 20) open(minecraft);
        int shot = (t - WARM_UP_TICKS - 40) / TICKS_PER_SHOT;
        int at = (t - WARM_UP_TICKS - 40) % TICKS_PER_SHOT;
        if (t < WARM_UP_TICKS + 40) return;
        if (shot >= shots.size()) {
            if (PLAY) REPORT.addAll(AdvancementPlayTest.results(server, player));
            finish(minecraft);
            return;
        }
        ResourceLocation id = shots.get(shot);
        if (at == 0) pointAt(minecraft, id);
        if (at == 5) {
            report(minecraft, server, id);
            Screenshot.grab(minecraft.gameDirectory, "selftest_advancement_" + id.getPath() + ".png", minecraft.getMainRenderTarget(), message -> {});
        }
    }

    /** Part of the tab: the root and a form, some animals and some curses, so progress shows. */
    private static void grantSome(MinecraftServer server) {
        run(server, "advancement revoke @a from dungeontotems:terms_and_conditions");
        run(server, "advancement grant @a only dungeontotems:terms_and_conditions");
        run(server, "advancement grant @a only dungeontotems:back_on_two_legs");
        for (String form : List.of("minecraft:cow", "minecraft:fox", "minecraft:axolotl", "minecraft:strider")) {
            run(server, "advancement grant @a only dungeontotems:great_and_small " + form);
        }
        run(server, "advancement grant @a only dungeontotems:waiting_it_out weakness");
        run(server, "advancement grant @a only dungeontotems:waiting_it_out blindness");
        run(server, "advancement grant @a only dungeontotems:close_calls void");
    }

    private static void open(Minecraft minecraft) {
        ClientAdvancements advancements = minecraft.player.connection.getAdvancements();
        minecraft.setScreen(new AdvancementsScreen(advancements));
        AdvancementNode root = advancements.getTree().get(DungeonTotems.id("terms_and_conditions"));
        if (root != null) advancements.setSelectedTab(root.holder(), true);
    }

    /** Moves the mouse onto the advancement's icon, as {@code AdvancementTab} centres and places it. */
    private static void pointAt(Minecraft minecraft, ResourceLocation id) {
        ClientAdvancements advancements = minecraft.player.connection.getAdvancements();
        List<DisplayInfo> tab = new ArrayList<>();
        for (ResourceLocation each : tab()) {
            AdvancementNode node = advancements.getTree().get(each);
            if (node != null) node.holder().value().display().ifPresent(tab::add);
        }
        AdvancementNode node = advancements.getTree().get(id);
        if (node == null || node.holder().value().display().isEmpty() || tab.isEmpty()) {
            REPORT.add(id + ": not on the screen");
            return;
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (DisplayInfo display : tab) {
            int x = Mth.floor(display.getX() * 28.0F), y = Mth.floor(display.getY() * 27.0F);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x + 28);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y + 27);
        }
        DisplayInfo display = node.holder().value().display().get();
        int left = (minecraft.getWindow().getGuiScaledWidth() - WINDOW_WIDTH) / 2;
        int top = (minecraft.getWindow().getGuiScaledHeight() - WINDOW_HEIGHT) / 2;
        int x = left + INSIDE_X + (117 - (maxX + minX) / 2) + Mth.floor(display.getX() * 28.0F) + 13;
        int y = top + INSIDE_Y + (56 - (maxY + minY) / 2) + Mth.floor(display.getY() * 27.0F) + 13;
        // The mouse is in window points (GameRenderer scales it to the GUI), not framebuffer pixels.
        setMouse(minecraft.mouseHandler, x * (double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
            y * (double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight());
    }

    /** The tooltip text the widget draws, from the same code it uses. */
    private static void report(Minecraft minecraft, MinecraftServer server, ResourceLocation id) {
        AdvancementHolder holder = server.getAdvancements().get(id);
        ServerPlayer player = server.getPlayerList().getPlayer(minecraft.player.getUUID());
        if (holder == null || player == null || holder.value().display().isEmpty()) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        AdvancementTooltips.Lines lines = AdvancementTooltips.lines(holder, holder.value().display().get(), progress);
        StringBuilder text = new StringBuilder(id.getPath()).append(" (").append(progress.isDone() ? "earned" : progress.getProgressText() == null ? "not earned" : progress.getProgressText().getString()).append("): ");
        if (lines == null) {
            text.append("vanilla tooltip");
        } else {
            for (FormattedCharSequence line : AdvancementTooltips.layout(minecraft.font, lines, 240, (component, width) -> minecraft.font.getSplitter().splitLines(component, width, Style.EMPTY))) {
                StringBuilder plain = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    plain.appendCodePoint(codePoint);
                    return true;
                });
                text.append("\n    ").append(plain);
            }
        }
        REPORT.add(text.toString());
    }

    private static void setMouse(MouseHandler mouse, double x, double y) {
        try {
            Field xpos = MouseHandler.class.getDeclaredField("xpos");
            Field ypos = MouseHandler.class.getDeclaredField("ypos");
            xpos.setAccessible(true);
            ypos.setAccessible(true);
            xpos.setDouble(mouse, x);
            ypos.setDouble(mouse, y);
        } catch (ReflectiveOperationException e) {
            REPORT.add("couldn't move the mouse: " + e);
        }
    }

    private static void finish(Minecraft minecraft) {
        try {
            Path out = minecraft.gameDirectory.toPath().resolve("selftest");
            Files.createDirectories(out);
            Files.write(out.resolve("advancements.txt"), REPORT);
        } catch (IOException e) {
            DungeonTotems.LOGGER.error("Advancement self-test report couldn't be written", e);
        }
        DungeonTotems.LOGGER.info("Advancement self-test done:\n{}", String.join("\n", REPORT));
        minecraft.stop();
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Every advancement in the tab. */
    private static List<ResourceLocation> tab() {
        List<ResourceLocation> all = new ArrayList<>(TabComplete.TAB);
        all.add(TabComplete.FULFILLED);
        return all;
    }
}
