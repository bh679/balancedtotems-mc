package games.brennan.balancedtotems.morph;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import games.brennan.balancedtotems.BalancedTotems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * {@code /morph next|prev|<form>|off}: become a morph form without a totem, for testing (ops only). Forms
 * go in id order; {@code next} after the last wraps to the first. No curse is applied, so the form lasts
 * until {@code /morph off} or a relog.
 */
@EventBusSubscriber(modid = BalancedTotems.MOD_ID)
public final class MorphCommand {

    private static final int OP_LEVEL = 2;
    private static final DynamicCommandExceptionType NOT_A_FORM = new DynamicCommandExceptionType(
        id -> Component.translatableWithFallback("commands.balancedtotems.morph.not_a_form", "%s isn't a morph form", id));

    private MorphCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("morph")
            .requires(source -> source.hasPermission(OP_LEVEL))
            .then(Commands.literal("next").executes(context -> step(context, 1)))
            .then(Commands.literal("prev").executes(context -> step(context, -1)))
            .then(Commands.literal("off").executes(context -> become(context.getSource().getPlayerOrException(), null, context)))
            .then(Commands.argument("form", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(forms().stream().map(MorphCommand::id), builder))
                .executes(context -> {
                    ResourceLocation id = ResourceLocationArgument.getId(context, "form");
                    EntityType<?> form = forms().stream().filter(type -> id(type).equals(id)).findFirst()
                        .orElseThrow(() -> NOT_A_FORM.create(id));
                    return become(context.getSource().getPlayerOrException(), form, context);
                })));
    }

    /** Every form in the {@code morph_forms} data map, in id order. */
    static List<EntityType<?>> forms() {
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(type -> Morphs.formData(type).isPresent())
            .sorted(Comparator.comparing(type -> id(type).toString())).toList();
    }

    private static int step(CommandContext<CommandSourceStack> context, int by) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<EntityType<?>> forms = forms();
        int current = forms.indexOf(Morphs.formOf(player).orElse(null));
        int next = current < 0 ? (by > 0 ? 0 : forms.size() - 1) : Math.floorMod(current + by, forms.size());
        return become(player, forms.get(next), context);
    }

    private static int become(ServerPlayer player, @Nullable EntityType<?> form, CommandContext<CommandSourceStack> context) {
        Morphs.setForm(player, form);
        List<EntityType<?>> forms = forms();
        Component message = form == null
            ? Component.translatableWithFallback("commands.balancedtotems.morph.off", "Back to yourself")
            : Component.translatableWithFallback("commands.balancedtotems.morph.now", "Now a %s (%s of %s)",
                form.getDescription(), forms.indexOf(form) + 1, forms.size());
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static ResourceLocation id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }
}
