package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.Curse;
import games.brennan.dungeontotems.DungeonTotems;
import games.brennan.dungeontotems.registry.TotemRegistries;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rage and Lonely show their timer on the effect box's own border: it starts lit in the curse's colour
 * and drains back to grey clockwise from the top. Once the time is up, the border flashes with a glow
 * just outside it until you hit something or give a gift. Same on the HUD and in the inventory.
 * Registered on the mod bus by {@link DungeonTotems}, on the client only.
 */
public final class TimerCurseIcons {

    private static final int RAGE_COLOUR = 0xFFDC2828;
    private static final int LONELY_COLOUR = 0xFFF082AA;
    /** The glow outside the box is the same colour at half strength. */
    private static final int GLOW_ALPHA = 0x80000000;
    private static final long FLASH_MILLIS = 250;
    /** HUD effect box ({@code hud/effect_background}). */
    private static final int HUD_BOX = 24;
    /** Inventory effect panels ({@code effect_background_large} / {@code _small}), and the icon's inset in each. */
    private static final int PANEL_HEIGHT = 32;
    private static final int LARGE_PANEL = 120;
    private static final int SMALL_PANEL = 32;
    private static final int LARGE_ICON_INSET = 6;
    private static final int SMALL_ICON_INSET = 7;

    private TimerCurseIcons() {}

    public static void onRegister(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new Timed(Curse.RAMPAGE, RAGE_COLOUR), TotemRegistries.RAMPAGE.get());
        event.registerMobEffect(new Timed(Curse.LONELINESS, LONELY_COLOUR), TotemRegistries.LONELY.get());
    }

    private record Timed(Curse curse, int colour) implements IClientMobEffectExtensions {

        @Override
        public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics, int x, int y, float z, float alpha) {
            drawCountdown(graphics, x, y, HUD_BOX, HUD_BOX, curse, colour);
            return false; // vanilla draws the icon on top
        }

        @Override
        public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics graphics,
                                           int x, int y, int blitOffset) {
            int panelsAt = screen.getGuiLeft() + screen.getXSize() + 2;
            if (x - LARGE_ICON_INSET == panelsAt) {
                drawCountdown(graphics, panelsAt, y, LARGE_PANEL, PANEL_HEIGHT, curse, colour);
            } else if (x - SMALL_ICON_INSET == panelsAt) {
                drawCountdown(graphics, panelsAt, y, SMALL_PANEL, PANEL_HEIGHT, curse, colour);
            }
            return false;
        }
    }

    /** Lights the part of the box's border still left on the timer, or flashes it all with a glow when the time is up. */
    static void drawCountdown(GuiGraphics graphics, int x, int y, int width, int height, Curse curse, int colour) {
        float left = ClientPayloads.fractionLeft(curse);
        List<int[]> border = border(width, height);
        if (left <= 0.0F) {
            if ((System.currentTimeMillis() / FLASH_MILLIS) % 2 == 1) return;
            int glow = (colour & 0x00FFFFFF) | GLOW_ALPHA;
            graphics.fill(x - 1, y, x, y + height, glow);
            graphics.fill(x + width, y, x + width + 1, y + height, glow);
            graphics.fill(x, y - 1, x + width, y, glow);
            graphics.fill(x, y + height, x + width, y + height + 1, glow);
            for (int[] p : border) graphics.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, colour);
            return;
        }
        int lit = Math.round(border.size() * left);
        // Drains clockwise from the top: the pixels still lit are the last ones round the ring.
        for (int i = border.size() - lit; i < border.size(); i++) {
            int[] p = border.get(i);
            graphics.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, colour);
        }
    }

    /**
     * The box's grey border ring, one pixel in from its edge with rounded corners, in clockwise order
     * starting at the top centre.
     */
    static List<int[]> border(int width, int height) {
        List<int[]> ring = new ArrayList<>();
        for (int x = 2; x <= width - 3; x++) {
            ring.add(new int[] {x, 1});
            ring.add(new int[] {x, height - 2});
        }
        for (int y = 2; y <= height - 3; y++) {
            ring.add(new int[] {1, y});
            ring.add(new int[] {width - 2, y});
        }
        ring.add(new int[] {2, 2});
        ring.add(new int[] {width - 3, 2});
        ring.add(new int[] {2, height - 3});
        ring.add(new int[] {width - 3, height - 3});
        double cx = (width - 1) / 2.0;
        double cy = (height - 1) / 2.0;
        ring.sort(Comparator.comparingDouble(p -> (Math.atan2(p[0] - cx, cy - p[1]) + 2 * Math.PI) % (2 * Math.PI)));
        return ring;
    }
}
