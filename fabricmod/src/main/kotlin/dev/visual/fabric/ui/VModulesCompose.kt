package dev.visual.fabric.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.visual.fabric.HudEditorScreen
import dev.visual.fabric.ModBrowserScreen
import net.minecraft.client.MinecraftClient
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.Theme

/**
 * Der Modul-Bildschirm, gezeichnet von OneConfigs Renderer.
 *
 * Der alte war Java mit DrawContext, und die Kacheln trugen Zeichen wie
 * Kreuz, Schild und Hand. Minecrafts Schrift kennt die nicht - im Spiel
 * stand dort grobe Pixelgrafik oder ein Platzhalter. Genau das laesst
 * einen Client billig aussehen.
 *
 * Hier kommen die Symbole aus den SVGs in assets/visualsfabric/ico (aus
 * OneLauncher, GPL-3.0, Herkunft siehe ATTRIBUTION.md), Schrift und
 * Farben aus OneConfigs Theme.
 *
 * Loest {@link VisualHomeScreen} ab, der als Rueckfall bleibt.
 */
class VModulesScreen : ComposeScreen() {

    private companion object {
        const val ICO = "/assets/visualsfabric/ico/"

        /**
         * Welches Symbol zu welchem Modul.
         *
         * Was nicht in der Liste steht, bekommt ein neutrales Quadrat -
         * lieber schlicht als wieder ein Platzhalter-Kaestchen.
         */
        val SYMBOL = mapOf(
            "crosshair" to "plus",
            "armor" to "onboarding-complete",
            "hands" to "onboarding-account",
            "hud" to "layout-top",
            "vanillahud" to "layout-top",
            "radar" to "globe-01",
            "combattimer" to "clock-rewind",
            "hitmarker" to "x-close",
            "damage" to "alert-triangle",
            "armorhud" to "bar-chart-square-02",
            "potions" to "colors",
            "scoreboard" to "dots-grid",
            "keystrokes" to "terminal",
            "counters" to "line-chart-up-01",
            "zoom" to "search-md",
            "freelook" to "refresh-cw-01",
            "fullbright" to "eye",
            "sky" to "moon-01",
            "minime" to "users-01",
            "fakeplayer" to "onboarding-account",
            "titlescreen" to "maximize-01",
            "menukey" to "terminal",
            "watermark" to "rocket-02",
            "clock" to "clock-rewind",
            "target" to "plus",
            "items" to "folder",
            "mods" to "download-01",
            "cosmetics" to "brush-01",
            "packs" to "folder-check"
        )

        /** Welche Module unter welcher Rubrik erscheinen. */
        val RUBRIK = mapOf(
            "hud" to setOf(
                "hud", "vanillahud", "armorhud", "potions", "scoreboard",
                "keystrokes", "counters", "clock", "items", "watermark"
            ),
            "kampf" to setOf(
                "combattimer", "hitmarker", "damage", "armor", "target", "radar"
            ),
            "sicht" to setOf(
                "zoom", "freelook", "fullbright", "sky", "crosshair", "hands"
            ),
            "cosmetics" to setOf("cosmetics", "minime", "fakeplayer", "packs")
        )
    }

    /**
     * ESC fuehrt zurueck, nicht ins Nichts.
     *
     * Minecrafts Screen.close() macht setScreen(null). Im Spiel ist das
     * richtig - im Titelbildschirm gibt es dann aber gar keinen
     * Bildschirm mehr, und man haengt fest. Genau das ist passiert.
     */
    override fun close() {
        val client = MinecraftClient.getInstance()
        if (client.world == null) {
            client.setScreen(VisualTitleScreen.oeffnen())
        } else {
            client.setScreen(null)
        }
    }

    private val mc: MinecraftClient get() = MinecraftClient.getInstance()

    /** Einmal aufbauen: all() legt bei jedem Aufruf neue Objekte an. */
    private val module: List<VModule> by lazy {
        try {
            ModuleRegistry.all(this)
        } catch (fehler: Throwable) {
            emptyList()
        }
    }

    private fun symbolFuer(id: String): String = ICO + (SYMBOL[id] ?: "square") + ".svg"

    @Composable
    override fun compose() = Theme { inhalt() }

    @Composable
    private fun inhalt() {
        val theme = LocalTheme.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Alles haengt an der Fensterhoehe, damit es bei jeder
            // Aufloesung gleich wirkt - dieselbe Lehre wie beim
            // Startbildschirm, wo dp den Aufbau gesprengt hat.
            val e = maxHeight * 0.042f
            val form = RoundedCornerShape(e * 0.5f)

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.80f)
                    .fillMaxHeight(0.78f)
                    .clip(form)
                    .background(theme.pageBackground)
                    .border(1.dp, theme.borderColor, form)
            ) {
                Seitenleiste(e)
                Inhalt(e)
            }
        }
    }

    @Composable
    private fun Seitenleiste(e: Dp) {
        val theme = LocalTheme.current
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(e * 6.4f)
                .background(theme.sidebarBackground)
                .padding(e * 0.5f)
        ) {
            Row {
                Text(
                    "VISUAL ", color = theme.textColor,
                    fontSize = (e.value * 0.42f).sp, fontWeight = FontWeight.Bold
                )
                Text(
                    "CLIENT", color = theme.accentTextColor,
                    fontSize = (e.value * 0.42f).sp, fontWeight = FontWeight.Bold
                )
            }
            Rubrik(VText.t("ui.settings"), e)
            Eintrag("sliders-04", VText.t("ui.modules"), true, e) { }
            Eintrag("layout-top", VText.t("ui.hudeditor"), false, e) {
                mc.setScreen(HudEditorScreen(this@VModulesScreen))
            }
            Rubrik(VText.t("ui.look"), e)
            Eintrag("brush-01", VText.t("ui.cosmetics"), false, e) {
                mc.setScreen(VisualCosmeticsScreen.oeffnen())
            }
            Rubrik(VText.t("ui.more"), e)
            Eintrag("download-01", VText.t("ui.modbrowser"), false, e) {
                mc.setScreen(ModBrowserScreen(this@VModulesScreen, "mod"))
            }
            Box(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ICO + "onboarding-account.svg", color = theme.textColor,
                    modifier = Modifier.size(e * 0.9f)
                )
                Column(Modifier.padding(start = e * 0.3f)) {
                    Text(
                        mc.session.username, color = theme.textColor,
                        fontSize = (e.value * 0.34f).sp
                    )
                    Text(
                        "Fabric " + (mc.gameVersion ?: ""),
                        color = theme.textColorSecondary,
                        fontSize = (e.value * 0.28f).sp
                    )
                }
            }
        }
    }

    @Composable
    private fun Rubrik(titel: String, e: Dp) {
        Text(
            titel.uppercase(),
            color = LocalTheme.current.textColorSecondary,
            fontSize = (e.value * 0.28f).sp,
            modifier = Modifier.padding(top = e * 0.6f, bottom = e * 0.2f)
        )
    }

    @Composable
    private fun Eintrag(symbol: String, titel: String, aktiv: Boolean, e: Dp, tun: () -> Unit) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        val form = RoundedCornerShape(e * 0.22f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(e * 1.1f)
                .clip(form)
                .background(if (aktiv) theme.accentTextColor else theme.sidebarBackground)
                .onClick(quelle, tun)
                .padding(horizontal = e * 0.3f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ICO + symbol + ".svg", color = theme.textColor, modifier = Modifier.size(e * 0.5f))
            Text(
                titel, color = theme.textColor, fontSize = (e.value * 0.36f).sp,
                modifier = Modifier.padding(start = e * 0.3f)
            )
        }
    }

    @Composable
    private fun Inhalt(e: Dp) {
        val theme = LocalTheme.current
        var rubrik by remember { mutableStateOf("alle") }
        val sichtbar = remember(rubrik) {
            if (rubrik == "alle") module
            else module.filter { RUBRIK[rubrik]?.contains(it.id) == true }
        }

        Column(modifier = Modifier.fillMaxSize().padding(e * 0.6f)) {
            Text(
                VText.t("ui.modules"), color = theme.textColor,
                fontSize = (e.value * 0.52f).sp, fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.padding(top = e * 0.4f, bottom = e * 0.5f),
                horizontalArrangement = Arrangement.spacedBy(e * 0.25f)
            ) {
                for ((schluessel, name) in listOf(
                    "alle" to VText.t("ui.all"),
                    "hud" to "HUD",
                    "kampf" to VText.t("ui.combat"),
                    "sicht" to VText.t("ui.view"),
                    "cosmetics" to VText.t("ui.cosmetics")
                )) {
                    Chip(name, rubrik == schluessel, e) { rubrik = schluessel }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(e * 0.3f),
                verticalArrangement = Arrangement.spacedBy(e * 0.3f),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sichtbar) { m -> Kachel(m, e) }
            }
        }
    }

    @Composable
    private fun Chip(name: String, aktiv: Boolean, e: Dp, tun: () -> Unit) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        val form = RoundedCornerShape(e * 0.5f)
        Box(
            modifier = Modifier
                .clip(form)
                .background(if (aktiv) theme.accentTextColor else theme.chipBackground)
                .onClick(quelle, tun)
                .padding(horizontal = e * 0.4f, vertical = e * 0.16f)
        ) {
            Text(name, color = theme.textColor, fontSize = (e.value * 0.32f).sp)
        }
    }

    /**
     * Eine Modulkachel: Symbol gross, Name darunter.
     *
     * Ein Klick schaltet um, und der Rahmen faerbt sich - bei 29
     * Kacheln sieht man den Zustand so auf einen Blick, ohne dass
     * irgendwo ein Haekchen klebt.
     */
    @Composable
    private fun Kachel(m: VModule, e: Dp) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        val form = RoundedCornerShape(e * 0.3f)
        var an by remember(m.id) {
            mutableStateOf(runCatching { m.enabled.asBoolean }.getOrDefault(false))
        }
        Column(
            modifier = Modifier
                .height(e * 2.8f)
                .clip(form)
                .background(theme.modCardBackground)
                .border(
                    if (an) 2.dp else 1.dp,
                    if (an) theme.accentTextColor else theme.borderColor,
                    form
                )
                .onClick(quelle) {
                    an = !an
                    runCatching { m.setEnabled.accept(an) }
                }
                .padding(e * 0.3f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(symbolFuer(m.id), color = theme.textColor, modifier = Modifier.size(e * 0.85f))
            Text(
                m.title, color = theme.textColor,
                fontSize = (e.value * 0.32f).sp,
                modifier = Modifier.padding(top = e * 0.25f)
            )
        }
    }
}
