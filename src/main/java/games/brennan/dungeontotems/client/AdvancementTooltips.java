package games.brennan.dungeontotems.client;

import games.brennan.dungeontotems.DungeonTotems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * What a Dungeon Totems advancement's tooltip says (drawn by {@code mixin/client/AdvancementWidgetMixin}):
 * <ul>
 *   <li>Until it's earned, its hint ({@code <key>.hint}) where it has one, in place of the description.</li>
 *   <li>Great and Small lists its animals: those you've been by name, the rest as {@code ????}.</li>
 * </ul>
 */
public final class AdvancementTooltips {

    public static final ResourceLocation GREAT_AND_SMALL = DungeonTotems.id("great_and_small");
    /** The animal list wraps no narrower than this, so 42 names stay a short block. */
    private static final int LIST_WIDTH = 240;

    /** The tooltip's text: what it says, then (for Great and Small) the animal list. */
    public record Lines(Component text, @Nullable Component list) {}

    private AdvancementTooltips() {}

    /** The tooltip for this advancement, or null to leave vanilla's. */
    @Nullable
    public static Lines lines(AdvancementHolder holder, DisplayInfo display, AdvancementProgress progress) {
        if (!holder.id().getNamespace().equals(DungeonTotems.MOD_ID)) return null;
        Style colour = Style.EMPTY.withColor(display.getType().getChatColor());
        Component text = display.getDescription();
        String hintKey = hintKey(text);
        if (!progress.isDone() && hintKey != null && I18n.exists(hintKey)) text = Component.translatable(hintKey);
        Component list = holder.id().equals(GREAT_AND_SMALL) ? animals(holder, progress) : null;
        return new Lines(ComponentUtils.mergeStyles(text.copy(), colour), list);
    }

    /** {@code advancements.dungeontotems.x.description} → {@code advancements.dungeontotems.x.hint}. */
    @Nullable
    static String hintKey(Component description) {
        ComponentContents contents = description.getContents();
        if (!(contents instanceof TranslatableContents translatable) || !translatable.getKey().endsWith(".description")) return null;
        String key = translatable.getKey();
        return key.substring(0, key.length() - ".description".length()) + ".hint";
    }

    /** Every animal of Great and Small, by name once you've been it. */
    private static Component animals(AdvancementHolder holder, AdvancementProgress progress) {
        MutableComponent list = Component.empty();
        // The client gets an advancement's requirements but not its criteria: one requirement per form.
        List<String> forms = new ArrayList<>(holder.value().requirements().names());
        forms.sort(null);
        for (int i = 0; i < forms.size(); i++) {
            if (i > 0) list.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            CriterionProgress criterion = progress.getCriterion(forms.get(i));
            ResourceLocation id = ResourceLocation.tryParse(forms.get(i));
            boolean been = criterion != null && criterion.isDone() && id != null;
            list.append(been
                ? BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().copy().withStyle(ChatFormatting.WHITE)
                : Component.translatable("advancements.dungeontotems.great_and_small.unknown").withStyle(ChatFormatting.DARK_GRAY));
        }
        return list;
    }

    public static int minWidth(Lines lines) {
        return lines.list() == null ? 0 : LIST_WIDTH;
    }

    /** The tooltip broken into lines: the text as vanilla breaks it, then the list wrapped to {@code width}. */
    public static List<FormattedCharSequence> layout(Font font, Lines lines, int width,
                                                     BiFunction<Component, Integer, List<FormattedText>> breakText) {
        List<FormattedText> out = new ArrayList<>(breakText.apply(lines.text(), width));
        if (lines.list() != null) {
            out.add(FormattedText.EMPTY);
            out.addAll(font.getSplitter().splitLines(lines.list(), width, Style.EMPTY));
        }
        return Language.getInstance().getVisualOrder(out);
    }
}
