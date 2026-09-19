package dev.visual.legacy;

import net.minecraft.client.resource.language.I18n;

/**
 * Texte in der Sprache des Spielers. Wie im neuen Mod: Minecrafts eigenes
 * I18n liest assets/visualsfabric/lang/<code>.json und faellt auf en_us
 * zurueck. Alle Schluessel beginnen mit vc.
 */
public final class VText189 {
    private VText189() {
    }

    public static String t(String schluessel) {
        return I18n.translate("vc." + schluessel);
    }
}
