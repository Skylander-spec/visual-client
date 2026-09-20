package dev.visual.fabric.ui;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/**
 * Style.withFont nimmt bis 1.21.8 einen Identifier, ab 1.21.9 eine
 * StyleSpriteSource. Deshalb steht die eine Zeile hier im Compat-Teil.
 */
public final class VFontCompat {
    private VFontCompat() {
    }

    public static Style schrift(Identifier id) {
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(id));
    }
}
