package dev.visual.fabric.ui

import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassLight
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UIBranding
import org.polyfrost.oneconfig.internal.ui.themes.UITheme

/**
 * Eigene Themes in OneConfigs Menue.
 *
 * Das Menue zeichnet OneConfig, und im Auswahlbildschirm stand deshalb
 * ihr Standard: "PolyGlass Dark". ThemeRegistry.register ist oeffentlich,
 * also legen wir ein eigenes daneben und schalten es ein - unser Name,
 * unser Logo. Ihre Themes bleiben unangetastet stehen; wer sie will,
 * waehlt sie weiter aus.
 *
 * copy() auf ihrem Theme statt einer eigenen Liste aus dreissig Farben:
 * so erben wir jede Farbe, die sie spaeter hinzufuegen, und muessen nur
 * benennen, was bei uns wirklich anders ist.
 */
object VisualTheme {
    private val MARKE = UIBranding("assets/visualsfabric/textures/logo.svg")

    val dunkel: UITheme = PolyGlassDark.copy(
        previewImage = "visual-dark",
        name = "Visual Dark",
        branding = MARKE
    )

    val hell: UITheme = PolyGlassLight.copy(
        previewImage = "visual-light",
        name = "Visual Light",
        branding = MARKE
    )

    /**
     * Einmal beim Start anmelden.
     *
     * Faengt breit ab: OneConfig ist optional, und wenn es fehlt oder
     * seine internen Klassen sich verschoben haben, darf das den Mod
     * nicht mitreissen - dann steht eben ihr Theme da.
     */
    @JvmStatic
    fun anmelden() {
        try {
            ThemeRegistry.register(dunkel)
            ThemeRegistry.register(hell)
            ThemeRegistry.activate(dunkel)
        } catch (fehler: Throwable) {
            System.err.println("[Visual] Theme nicht anmeldbar: $fehler")
        }
    }
}
