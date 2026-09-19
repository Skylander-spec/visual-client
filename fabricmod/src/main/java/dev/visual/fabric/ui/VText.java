package dev.visual.fabric.ui;

import net.minecraft.client.resource.language.I18n;

/**
 * Texte der Client-Oberfläche in der Sprache des Spielers.
 *
 * Minecraft bringt das Nötige mit: {@code I18n.translate} liest die
 * Sprachdatei unter {@code assets/visualsfabric/lang/<code>.json} und fällt
 * auf {@code en_us} zurück, wenn ein Eintrag fehlt. Deshalb kein eigenes
 * Sprachsystem — nur diese Abkürzung, damit der Zeichencode lesbar bleibt.
 *
 * Alle Schlüssel beginnen mit {@code vc.}; hier steht nur der Rest.
 */
public final class VText {
    private VText() {
    }

    public static String t(String schluessel, Object... werte) {
        return I18n.translate("vc." + schluessel, werte);
    }
}
