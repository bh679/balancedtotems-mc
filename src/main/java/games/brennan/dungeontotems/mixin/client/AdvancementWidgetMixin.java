package games.brennan.dungeontotems.mixin.client;

import games.brennan.dungeontotems.client.AdvancementTooltips;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Dungeon Totems advancements' tooltips ({@link AdvancementTooltips}): a hint in place of the description
 * until earned, and Great and Small's list of animals. Rebuilt whenever the progress changes.
 */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow @Final private AdvancementNode advancementNode;
    @Shadow @Final private DisplayInfo display;
    @Shadow @Final private FormattedCharSequence title;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final @Mutable private List<FormattedCharSequence> description;
    @Shadow @Final @Mutable private int width;

    @Shadow private int getMaxProgressWidth() { throw new AssertionError(); }

    @Shadow private List<FormattedText> findOptimalLines(Component component, int maxWidth) { throw new AssertionError(); }

    @Inject(method = "setProgress", at = @At("TAIL"))
    private void dungeontotems$tooltip(AdvancementProgress progress, CallbackInfo ci) {
        AdvancementTooltips.Lines lines = AdvancementTooltips.lines(advancementNode.holder(), display, progress);
        if (lines == null) return;
        int wrap = 29 + minecraft.font.width(title) + getMaxProgressWidth();
        List<FormattedCharSequence> text = AdvancementTooltips.layout(minecraft.font, lines,
            Math.max(wrap, AdvancementTooltips.minWidth(lines)), this::findOptimalLines);
        int widest = wrap;
        for (FormattedCharSequence line : text) widest = Math.max(widest, minecraft.font.width(line));
        description = text;
        width = widest + 3 + 5;
    }
}
