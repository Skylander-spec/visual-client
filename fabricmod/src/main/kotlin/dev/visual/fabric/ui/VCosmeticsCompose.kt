package dev.visual.fabric.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.visual.fabric.CapeManager
import dev.visual.fabric.Compat
import net.minecraft.client.MinecraftClient
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Cosmetics an einem Ort, gezeichnet von OneConfigs Renderer.
 *
 * Alle Masse haengen an der Fensterhoehe, nicht an dp. Genau daran ist
 * dieser Bildschirm gescheitert: ComposeScreen bekommt Minecrafts
 * GUI-Faktor als Dichte, und bei Faktor 4 werden aus 260.dp Spiegel-
 * breite 1040 Pixel und aus GridCells.Adaptive(150.dp) eine Mindest-
 * breite von 600. Er ging schlicht nicht mehr auf, ohne eine Zeile
 * Ausgabe. Start- und Modulbildschirm waren laengst umgestellt, dieser
 * hier war uebersehen worden.
 *
 * Umhaenge, Mini-Me und Accessoires stehen in Rubriken nebeneinander
 * statt auf mehreren Bildschirmen verteilt, mit dem Spiegel daneben.
 */
class VCosmeticsScreen : ComposeScreen() {

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

    /** Ein Eintrag im Raster. Das Bild bleibt roh, damit Compose es laden kann. */
    private class Stueck(val name: String, val png: ByteArray?)

    /**
     * Die Dateien einmal beim ersten Zugriff lesen.
     *
     * Bewusst nicht in compose(): das laeuft pro Bild, und von der Platte
     * zu lesen wuerde dabei jedes Mal stocken.
     */
    private val umhaenge: List<Stueck> by lazy { lesen("capes") }
    private val accessoires: List<Stueck> by lazy { lesen("accessoires") }

    private fun lesen(unterordner: String): List<Stueck> {
        val ordner: Path = CapeManager.datenOrdner().resolve(unterordner)
        if (!Files.isDirectory(ordner)) return emptyList()
        return try {
            Files.list(ordner).use { liste ->
                liste.toList()
                    .filter { it.fileName.toString().endsWith(".png", ignoreCase = true) }
                    .sortedBy { it.fileName.toString().lowercase() }
                    .map { pfad ->
                        val name = pfad.fileName.toString().removeSuffix(".png")
                        Stueck(name, runCatching { Files.readAllBytes(pfad) }.getOrNull())
                    }
            }
        } catch (fehler: Throwable) {
            emptyList()
        }
    }

    /**
     * Theme { } muss aussen herum: LocalTheme ist bei ihnen ein
     * compositionLocalOf mit error() als Standard, ohne Anbieter wirft
     * schon der erste Zugriff.
     */
    @Composable
    override fun compose() = Theme { inhalt() }

    @Composable
    private fun inhalt() {
        val theme = LocalTheme.current
        var rubrik by remember { mutableStateOf("umhaenge") }
        var gewaehlt by remember { mutableStateOf(CapeManager.angelegt() ?: "") }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    .padding(e * 0.5f),
                horizontalArrangement = Arrangement.spacedBy(e * 0.5f)
            ) {
                Spiegel(e, gewaehlt)
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        VText.t("ui.cosmetics"), color = theme.textColor,
                        fontSize = (e.value * 0.52f).sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.padding(top = e * 0.4f, bottom = e * 0.5f),
                        horizontalArrangement = Arrangement.spacedBy(e * 0.25f)
                    ) {
                        for ((schluessel, name) in listOf(
                            "umhaenge" to VText.t("ui.capes"),
                            "minime" to VText.t("mod.minime"),
                            "accessoires" to VText.t("ui.accessoires")
                        )) {
                            Chip(name, rubrik == schluessel, e) { rubrik = schluessel }
                        }
                    }
                    when (rubrik) {
                        "minime" -> Hinweis(VText.t("ui.minimehint"), e)
                        "accessoires" ->
                            if (accessoires.isEmpty()) Hinweis(VText.t("ui.accsoon"), e)
                            else Raster(accessoires, gewaehlt, e) { gewaehlt = it }
                        else -> Raster(umhaenge, gewaehlt, e) { gewaehlt = it }
                    }
                }
            }
        }
    }

    /**
     * Der Spiegel.
     *
     * Die Flaeche in der Mitte bleibt hier leer - dort zeichnet
     * Minecrafts eigener Renderer, siehe render(). Compose kann keine
     * Minecraft-Figur zeichnen, also wird sie darueber gelegt.
     */
    @Composable
    private fun Spiegel(e: Dp, gewaehlt: String) {
        val theme = LocalTheme.current
        val form = RoundedCornerShape(e * 0.3f)
        Column(
            modifier = Modifier
                .width(e * 6.4f)
                .fillMaxHeight()
                .clip(form)
                .background(theme.modCardBackground)
                .border(1.dp, theme.borderColor, form)
                .padding(e * 0.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(e * 0.3f)
        ) {
            Text(
                VText.t("ui.mirror"), color = theme.textColor,
                fontSize = (e.value * 0.4f).sp, fontWeight = FontWeight.SemiBold
            )
            // Bewusst leer: hier zeichnet Minecrafts Renderer den Avatar
            // ueber die Compose-Ebene, siehe render(). Compose selbst kann
            // keine Minecraft-Figur zeichnen.
            Box(modifier = Modifier.fillMaxWidth().weight(1f))
            Text(
                if (gewaehlt.isEmpty()) VText.t("ui.nothingworn") else gewaehlt,
                color = theme.textColorSecondary,
                fontSize = (e.value * 0.32f).sp
            )
        }
    }

    /**
     * Den Avatar ueber die Compose-Ebene zeichnen.
     *
     * Im Spiel die echte Figur ueber InventoryScreen.drawEntity - diese
     * Ueberladung gibt es wortgleich in 1.21.2 bis 1.21.11, mit javap in
     * allen geprueft, deshalb ohne Fallunterscheidung. Sie dreht sich mit
     * der Maus, wie im Inventar.
     *
     * Im Titelbildschirm gibt es keinen Spieler; dort zeichnet VAvatar
     * die Puppe aus derselben Skin-Textur.
     */
    override fun render(ctx: net.minecraft.client.gui.DrawContext, mausX: Int, mausY: Int, delta: Float) {
        super.render(ctx, mausX, mausY, delta)
        // Compose hinterlaesst eine Transformation auf dem Matrix-Stapel.
        // Ohne eigenen Rahmen zeichnet alles Folgende darin - die Figur
        // landete dadurch weit oberhalb des Spiegels, ausserhalb des
        // Fensters. Push/pop stellt den Ursprung wieder her.
        Compat.matrixAuf(ctx)
        try {
            val e = height * 0.042f
            val feldB = width * 0.80f
            val feldH = height * 0.78f
            val feldL = (width - feldB) / 2f
            val feldO = (height - feldH) / 2f
            val spiegelL = feldL + e * 0.5f
            val spiegelB = e * 6.4f
            val spiegelO = feldO + e * 0.5f
            val spiegelH = feldH - e

            // Zwischen Ueberschrift und Namenszeile
            val oben = (spiegelO + e * 1.5f).toInt()
            val unten = (spiegelO + spiegelH - e * 1.2f).toInt()
            val mitteX = (spiegelL + spiegelB / 2f).toInt()
            if (unten - oben < 8) return

            val spieler = MinecraftClient.getInstance().player
            if (spieler != null) {
                val groesse = ((unten - oben) / 2.4f).toInt().coerceAtLeast(1)
                net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                    ctx,
                    mitteX - spiegelB.toInt() / 3, oben,
                    mitteX + spiegelB.toInt() / 3, unten,
                    groesse, 0.0625f,
                    mausX.toFloat(), mausY.toFloat(),
                    spieler
                )
            } else {
                VAvatar.zeichne(ctx, mitteX, oben, unten - oben)
            }
        } catch (fehler: Throwable) {
            // Lieber ein leerer Spiegel als ein toter Bildschirm
        } finally {
            Compat.matrixZu(ctx)
        }
    }

    @Composable
    private fun Hinweis(text: String, e: Dp) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = LocalTheme.current.textColorSecondary,
                fontSize = (e.value * 0.36f).sp
            )
        }
    }

    @Composable
    private fun Raster(liste: List<Stueck>, gewaehlt: String, e: Dp, waehlen: (String) -> Unit) {
        // Feste Spaltenzahl statt Adaptive: Adaptive rechnet mit einer
        // Mindestbreite in dp, und die ist hier um den GUI-Faktor zu gross.
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(e * 0.3f),
            verticalArrangement = Arrangement.spacedBy(e * 0.3f),
            modifier = Modifier.fillMaxSize()
        ) {
            items(liste) { stueck ->
                Karte(stueck, stueck.name == gewaehlt, e) {
                    val neu = if (stueck.name == gewaehlt) "" else stueck.name
                    waehlen(neu)
                    runCatching { CapeManager.anlegen(neu) }
                }
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

    @Composable
    private fun Karte(stueck: Stueck, aktiv: Boolean, e: Dp, tun: () -> Unit) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        val form = RoundedCornerShape(e * 0.3f)
        Column(
            modifier = Modifier
                .height(e * 4.2f)
                .clip(form)
                .background(theme.modCardBackground)
                .border(
                    if (aktiv) 2.dp else 1.dp,
                    if (aktiv) theme.accentTextColor else theme.borderColor,
                    form
                )
                .onClick(quelle, tun)
                .padding(e * 0.25f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(e * 0.2f)
        ) {
            Vorschau(stueck.png, Modifier.weight(1f).aspectRatio(10f / 16f), e)
            Text(stueck.name, color = theme.textColor, fontSize = (e.value * 0.3f).sp)
        }
    }

    /**
     * Die Vorderseite eines Umhangs.
     *
     * Ein Cape-Blatt ist 64x32, die Vorderseite liegt bei (1,1) und ist
     * 10x16 gross; hoehere Aufloesungen sind Vielfache davon, daher der
     * Faktor aus der Breite. Der Ausschnitt wird auf das Bild begrenzt -
     * eine Datei, die kein Cape-Blatt ist, soll ein leeres Feld geben und
     * nicht den Bildschirm mitnehmen.
     */
    @Composable
    private fun Vorschau(png: ByteArray?, modifier: Modifier, e: Dp) {
        val theme = LocalTheme.current
        val form = RoundedCornerShape(e * 0.2f)
        val bild = remember(png) {
            png?.let { runCatching { loadImageBitmap(ByteArrayInputStream(it)) }.getOrNull() }
        }
        if (bild == null) {
            Box(modifier.clip(form).background(theme.componentBackground))
            return
        }
        val f = (bild.width / 64).coerceAtLeast(1)
        val breite = (10 * f).coerceAtMost(bild.width - f)
        val hoehe = (16 * f).coerceAtMost(bild.height - f)
        if (breite <= 0 || hoehe <= 0) {
            Box(modifier.clip(form).background(theme.componentBackground))
            return
        }
        Image(
            painter = BitmapPainter(bild, IntOffset(f, f), IntSize(breite, hoehe)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(form)
        )
    }
}
