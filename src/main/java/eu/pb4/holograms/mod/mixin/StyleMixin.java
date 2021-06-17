package eu.pb4.holograms.mod.mixin;

import eu.pb4.holograms.mod.util.FontHolder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


@Mixin(Style.class)
public class StyleMixin implements FontHolder {

    @Mutable
    @Shadow @Final @Nullable private Identifier font;

    @Shadow @Final @Nullable private TextColor color;

    @Shadow @Final @Nullable private Boolean bold;

    @Shadow @Final @Nullable private Boolean italic;

    @Shadow @Final @Nullable private Boolean underlined;

    @Shadow @Final @Nullable private Boolean strikethrough;

    @Shadow @Final @Nullable private Boolean obfuscated;

    @Shadow @Final @Nullable private ClickEvent clickEvent;

    @Shadow @Final @Nullable private HoverEvent hoverEvent;

    @Shadow @Final @Nullable private String insertion;

    //Crappy solution to be able to set the font
    @Override
    public Style setFont(Identifier font) {
        try {
            Constructor<Style> c = Style.class.getDeclaredConstructor(TextColor.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class, Boolean.class, ClickEvent.class, HoverEvent.class, String.class, Identifier.class);
            return c.newInstance(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, font);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return (Style) (Object)this;
        }
    }
}
