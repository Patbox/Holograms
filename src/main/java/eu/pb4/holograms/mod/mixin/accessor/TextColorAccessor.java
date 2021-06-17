package eu.pb4.holograms.mod.mixin.accessor;

import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextColor.class)
public interface TextColorAccessor {
    @Accessor("rgb")
    int getRGB();
}
