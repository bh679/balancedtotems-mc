package games.brennan.dungeontotems.mixin.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/** A model part's named children and its boxes, so a form's arm can be found and measured ({@code FormArms}). */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {

    @Accessor("children")
    Map<String, ModelPart> dungeontotems$children();

    @Accessor("cubes")
    List<ModelPart.Cube> dungeontotems$cubes();
}
