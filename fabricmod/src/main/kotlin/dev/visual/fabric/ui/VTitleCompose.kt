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

    @Composable
    private fun inhalt() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF070B12), Color(0xFF0D1620), Color(0xFF10161F))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().weight(0.42f)) {
                    Wortmarke(Modifier.align(Alignment.Center))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(0.58f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.24f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { Schnellstart() }
                    Column(
                        modifier = Modifier.weight(0.36f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { Mitte() }
                    Column(
                        modifier = Modifier.weight(0.24f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { Rechts() }
                }
            }
        }
    }

    @Composable
    private fun Wortmarke(modifier: Modifier) {
        val theme = LocalTheme.current
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon("star-filled", color = theme.textColor, modifier = Modifier.size(44.dp))
            // Eng und schwer gesetzt wie die Vorlage. Vorher war die
            // Wortmarke duenn und weit gesperrt - das las sich luftig
            // statt als Marke.
            Text(
                "VISUAL CLIENT",
                color = theme.textColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun Schnellstart() {
        val theme = LocalTheme.current
        Text(
            VText.t("ui.quickstart"),
            color = theme.textColorSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        for (s in server) {
            // Spielerzahl, wenn Minecraft sie schon kennt - sonst die
            // Adresse. Die Vorlage zeigt immer die Zahl, weil sie die
            // Server anpingt; das kommt spaeter.
            val unter = s.playerCountLabel?.string?.takeIf { it.isNotBlank() } ?: s.address
            Karte("cloud", s.name, unter, hoch = true) {
                ConnectScreen.connect(
                    mc.currentScreen, mc, ServerAddress.parse(s.address), s, false, null
                )
            }
        }
    }

    @Composable
    private fun Mitte() {
        Karte("profiles", VText.t("ui.singleplayer"), null) {
            mc.setScreen(SelectWorldScreen(mc.currentScreen))
        }
        Karte("cloud", VText.t("ui.multiplayer"), null) {
            mc.setScreen(MultiplayerScreen(mc.currentScreen))
        }
        Karte("settings", VText.t("ui.options"), null) {
            mc.setScreen(OptionsScreen(mc.currentScreen, mc.options))
        }
        Karte("layers", VText.t("ui.modules"), null) {
            mc.setScreen(VisualHomeScreen())
        }
    }

    @Composable
    private fun Rechts() {
        Karte("profiles", mc.session.username, null) { }
        Karte("star", VText.t("ui.cosmetics"), null) {
            mc.setScreen(VisualCosmeticsScreen.oeffnen())
        }
        Karte("close", VText.t("ui.quit"), null) { mc.scheduleStop() }
    }

    /**
     * Ein Knopf, wie die Vorlage ihn zeichnet: Symbol, Beschriftung,
     * leichter Rahmen, abgerundet nach dem Theme.
     */
    @Composable
    private fun Karte(
        symbol: String,
        titel: String,
        unter: String?,
        hoch: Boolean = false,
        tun: () -> Unit
    ) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (hoch) 42.dp else 30.dp)
                .clip(theme.buttonShape)
                .background(theme.componentBackground)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .onClick(quelle, tun)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (hoch) Arrangement.Start else Arrangement.Center
        ) {
            Icon(symbol, color = theme.textColor, modifier = Modifier.size(13.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(titel, color = theme.textColor, fontSize = 13.sp)
                if (unter != null) {
                    Text(unter, color = theme.textColorSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
