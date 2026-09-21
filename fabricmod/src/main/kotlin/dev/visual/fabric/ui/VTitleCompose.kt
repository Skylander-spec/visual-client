package dev.visual.fabric.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.option.ServerList
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.Theme

/**
 * Startbildschirm, gezeichnet von OneConfigs Renderer.
 *
 * Bisher war er in Java und mit DrawContext von Hand gemalt: Rechtecke
 * als Knoepfe, Symbole aus Strichen, Schrift ueber drawText. Das war der
 * Grund, warum er trotz stimmender Maesse nicht wie die Vorlage aussah -
 * einem gemalten Knopf sieht man an, dass er gemalt ist.
 *
 * Hier kommt alles aus OneConfig: Icon() laedt ihre SVGs, Text() setzt in
 * ihrer Schrift, LocalTheme liefert Farben und Formen. Uebrig bleibt der
 * Aufbau, und der ist unserer.
 *
 * PolyPlus, das die Vorlage zeichnet, steht unter PolyForm Shield mit
 * Noncompete-Klausel und ist deshalb nicht kopierbar. OneConfig ist
 * LGPL - deshalb kommt von dort alles, was von dort kommen kann.
 *
 * {@link VisualTitleScreen} bleibt als Rueckfall, falls OneConfig im
 * Profil fehlt.
 */
class VTitleScreen : ComposeScreen() {

    private companion object {
        /**
         * Unsere eigenen Symbole im Jar.
         *
         * OneConfigs Icon() loest einen Namen ohne '/' zu
         * /assets/oneconfig/ico/<name>.svg auf, einen mit '/' dagegen als
         * Pfad - so kommen wir an unsere eigenen heran. Die Dateien
         * stammen aus OneLauncher (GPL-3.0), Herkunft steht in
         * assets/visualsfabric/ico/ATTRIBUTION.md.
         */
        const val ICO = "/assets/visualsfabric/ico/"

        const val RAND = 0.0295f        // Seitenrand, links wie rechts
        const val SPALTE = 0.1538f      // Breite der Seitenspalten
        const val MITTE = 0.2270f       // Breite der Mittelspalte
        const val KNOPF_H = 0.0371f     // Knopfhoehe
        const val ZEILE = 0.0542f       // Abstand Zeile zu Zeile
        const val ZEILE1 = 0.5013f      // Oberkante der ersten Knopfreihe
        const val MARKE_Y = 0.3465f     // Mitte der Bildmarke
        const val KARTE_H = 0.0554f     // Hoehe einer Serverzeile
        const val KARTE_L = 0.0138f     // Luecke zwischen Serverzeilen
    }


    private val mc: MinecraftClient get() = MinecraftClient.getInstance()

    /**
     * Die drei Schnellstart-Server einmal lesen.
     *
     * Nicht in compose(): das laeuft pro Bild, und servers.dat bei jedem
     * Bild von der Platte zu lesen wuerde stocken.
     */
    private val server: List<ServerInfo> by lazy {
        try {
            val liste = ServerList(mc)
            liste.loadFile()
            (0 until minOf(3, liste.size())).map { liste.get(it) }
        } catch (fehler: Throwable) {
            emptyList()
        }
    }

    @Composable
    override fun compose() = Theme { inhalt() }

    /**
     * Der Aufbau in Anteilen der Fenstergroesse, nicht in dp.
     *
     * In dp gerechnet war alles doppelt so gross: ComposeScreen bekommt
     * Minecrafts GUI-Faktor als Dichte, und bei Faktor 3 werden aus
     * 30.dp eben 90 Pixel. Die rechte Spalte lag dann ausserhalb des
     * Bildes.
     *
     * Die Zahlen sind an OneClient abgemessen, als Anteil der Fenster-
     * breite beziehungsweise -hoehe. So sitzt alles bei jeder
     * Aufloesung und jedem GUI-Faktor an derselben Stelle wie dort.
     */

    @Composable
    private fun inhalt() {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF070B12), Color(0xFF0D1620), Color(0xFF10161F))
                    )
                )
        ) {
            val b = maxWidth
            val h = maxHeight
            val rand = b * RAND
            val spalte = b * SPALTE
            val mitte = b * MITTE
            val knopf = h * KNOPF_H
            val zeile = h * ZEILE

            Wortmarke(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = h * MARKE_Y - knopf * 2f),
                knopf
            )

            // Linke Spalte: Schnellstart
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = rand, y = h * ZEILE1 - knopf)
                    .width(spalte)
            ) { Schnellstart(h * KARTE_H, h * KARTE_L, knopf) }

            // Mittelspalte
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = h * ZEILE1)
                    .width(mitte),
                verticalArrangement = Arrangement.spacedBy(zeile - knopf)
            ) { Mitte(knopf) }

            // Rechte Spalte
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -rand, y = h * ZEILE1)
                    .width(spalte),
                verticalArrangement = Arrangement.spacedBy(zeile - knopf)
            ) { Rechts(knopf) }

            Fusszeile(Modifier.align(Alignment.BottomStart).offset(x = rand, y = -rand * 0.6f), knopf)

        }
    }

    /** Die Zeile unten links: Name und Plattform, wie in der Vorlage. */
    @Composable
    private fun Fusszeile(modifier: Modifier, knopf: Dp) {
        val theme = LocalTheme.current
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "VISUAL CLIENT",
                color = theme.textColor,
                fontSize = (knopf.value * 0.32f).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "  Fabric " + (mc.gameVersion ?: ""),
                color = theme.textColorSecondary,
                fontSize = (knopf.value * 0.32f).sp
            )
        }
    }

    @Composable
    private fun Wortmarke(modifier: Modifier, knopf: Dp) {
        val theme = LocalTheme.current
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(knopf * 0.35f)
        ) {
            Icon(
                ICO + "rocket-02.svg",
                color = theme.textColor,
                modifier = Modifier.size(knopf * 1.6f)
            )
            Text(
                "VISUAL CLIENT",
                color = theme.textColor,
                fontSize = (knopf.value * 0.85f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun Schnellstart(karte: Dp, luecke: Dp, knopf: Dp) {
        val theme = LocalTheme.current
        Text(
            VText.t("ui.quickstart"),
            color = theme.textColorSecondary,
            fontSize = (knopf.value * 0.36f).sp,
            modifier = Modifier.padding(bottom = luecke)
        )
        for (s in server) {
            // Spielerzahl, wenn Minecraft sie kennt - sonst die Adresse.
            val unter = s.playerCountLabel?.string?.takeIf { it.isNotBlank() } ?: s.address
            Karte(ICO + "globe-01.svg", s.name, unter, karte, knopf) {
                ConnectScreen.connect(
                    mc.currentScreen, mc, ServerAddress.parse(s.address), s, false, null
                )
            }
            Spacer(Modifier.height(luecke))
        }
    }

    /**
     * Die Mittelspalte wie in der Vorlage: drei Zeilen ueber die volle
     * Breite, darunter Optionen und Module nebeneinander. Vorher standen
     * alle vier untereinander und Realms fehlte ganz.
     */
    @Composable
    private fun Mitte(knopf: Dp) {
        Karte(ICO + "play.svg", VText.t("ui.singleplayer"), null, knopf, knopf) {
            mc.setScreen(SelectWorldScreen(mc.currentScreen))
        }
        Karte(ICO + "users-01.svg", VText.t("ui.multiplayer"), null, knopf, knopf) {
            mc.setScreen(MultiplayerScreen(mc.currentScreen))
        }
        Karte(ICO + "globe-01.svg", VText.t("ui.realms"), null, knopf, knopf) {
            mc.setScreen(RealmsMainScreen(mc.currentScreen))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(knopf * 0.28f)) {
            Box(Modifier.weight(1f)) {
                Karte(ICO + "settings-01.svg", VText.t("ui.options"), null, knopf, knopf) {
                    mc.setScreen(OptionsScreen(mc.currentScreen, mc.options))
                }
            }
            Box(Modifier.weight(1f)) {
                Karte(ICO + "sliders-04.svg", VText.t("ui.modules"), null, knopf, knopf) {
                    mc.setScreen(VisualHomeScreen.oeffnen())
                }
            }
        }
    }

    @Composable
    private fun Rechts(knopf: Dp) {
        Karte(ICO + "users-01.svg", mc.session.username, null, knopf, knopf) { }
        Karte(ICO + "brush-01.svg", VText.t("ui.cosmetics"), null, knopf, knopf) {
            mc.setScreen(VisualCosmeticsScreen.oeffnen())
        }
        Karte(ICO + "x-close.svg", VText.t("ui.quit"), null, knopf, knopf) { mc.scheduleStop() }
    }

    /**
     * Ein Knopf.
     *
     * Radius 8 und Rahmenbreite 1 stehen so in OneLaunchers
     * button.rs (size_layout, ButtonSize::Medium) - uebernommen statt
     * geschaetzt. Die Schriftgroesse haengt an der Knopfhoehe, damit das
     * Verhaeltnis bei jeder Aufloesung stimmt.
     */
    @Composable
    private fun Karte(
        symbol: String,
        titel: String,
        unter: String?,
        hoehe: Dp,
        knopf: Dp,
        tun: () -> Unit
    ) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        val form = RoundedCornerShape(knopf * 0.22f)
        val mittig = unter == null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(hoehe)
                .clip(form)
                .background(theme.componentBackground)
                .border(1.dp, theme.borderColor, form)
                .onClick(quelle, tun)
                .padding(horizontal = knopf * 0.35f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (mittig) Arrangement.Center else Arrangement.Start
        ) {
            Icon(symbol, color = theme.textColor, modifier = Modifier.size(knopf * 0.46f))
            Column(modifier = Modifier.padding(start = knopf * 0.28f)) {
                Text(titel, color = theme.textColor, fontSize = (knopf.value * 0.40f).sp)
                if (unter != null) {
                    Text(
                        unter,
                        color = theme.textColorSecondary,
                        fontSize = (knopf.value * 0.32f).sp
                    )
                }
            }
        }
    }
}
