package dev.visual.fabric.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.visual.fabric.CapeManager
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
 * Der Bildschirm erbt ihren ComposeScreen und nimmt ihre Bausteine:
 * Theme, Schrift, Text. Deshalb sieht er nicht aehnlich aus wie ihr
 * Menue, sondern gleich - es ist derselbe Renderer. Der Aufbau ist
 * unserer, den koennen wir frei aendern.
 *
 * Umhaenge und kleine Cosmetics stehen bewusst in einem Raster
 * zusammen statt auf zwei Seiten verteilt, mit dem Spiegel daneben.
 *
 * Loest {@link VisualCosmeticsScreen} ab, sobald er im Spiel steht.
 */
class VCosmeticsScreen : ComposeScreen() {

    /** Ein Eintrag im Raster. Das Bild bleibt roh, damit Compose es laden kann. */
    private class Stueck(val name: String, val png: ByteArray?)

    /**
     * Die Cape-Dateien einmal beim ersten Zugriff lesen.
     *
     * Bewusst nicht in compose(): das laeuft pro Bild, und von der Platte
     * zu lesen wuerde dabei jedes Mal stocken.
     */
    private val stuecke: List<Stueck> by lazy { lesen() }

    private fun lesen(): List<Stueck> {
        val ordner: Path = CapeManager.datenOrdner().resolve("capes")
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
     * Theme { } muss aussen herum.
     *
     * LocalTheme ist bei ihnen als
     * compositionLocalOf<UITheme> { error("A UI theme is required ...") }
     * angelegt - ohne Anbieter wirft schon der erste Zugriff. Der
     * Bildschirm ging dann gar nicht auf: Minecraft fiel sofort auf den
     * Titelbildschirm zurueck, und weil unser Mixin den wieder ersetzt,
     * sah es aus, als passiere beim Klick einfach nichts.
     */
    @Composable
    override fun compose() = Theme {
        inhalt()
    }

    @Composable
    private fun inhalt() {
        val theme = LocalTheme.current
        var gewaehlt by remember { mutableStateOf(CapeManager.angelegt() ?: "") }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.pageBackground)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .clip(theme.modCardShape)
                    .background(theme.modCardBackground)
                    .border(1.dp, theme.borderColor, theme.modCardShape)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Spiegel",
                    color = theme.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Vorschau(
                        stuecke.firstOrNull { it.name == gewaehlt }?.png,
                        Modifier.fillMaxHeight().aspectRatio(10f / 16f)
                    )
                }
                Text(
                    if (gewaehlt.isEmpty()) "Nichts angelegt" else gewaehlt,
                    color = theme.textColorSecondary,
                    fontSize = 13.sp
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    "Cosmetics",
                    color = theme.textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stuecke.size.toString() + " Stueck - Umhaenge und Kleinteile an einem Ort",
                    color = theme.textColorSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(stuecke) { stueck ->
                        Karte(
                            stueck = stueck,
                            aktiv = stueck.name == gewaehlt,
                            onClick = {
                                gewaehlt = if (stueck.name == gewaehlt) "" else stueck.name
                                CapeManager.anlegen(gewaehlt)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Karte(stueck: Stueck, aktiv: Boolean, onClick: () -> Unit) {
        val theme = LocalTheme.current
        val quelle = rememberInteractionSource()
        Column(
            modifier = Modifier
                .clip(theme.modCardShape)
                .background(theme.modCardBackground)
                .border(
                    if (aktiv) 2.dp else 1.dp,
                    if (aktiv) theme.accentTextColor else theme.borderColor,
                    theme.modCardShape
                )
                .onClick(quelle, onClick)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Vorschau(stueck.png, Modifier.fillMaxWidth().aspectRatio(10f / 16f))
            Text(stueck.name, color = theme.textColor, fontSize = 13.sp)
        }
    }

    /**
     * Die Vorderseite eines Umhangs.
     *
     * Eine Cape-Datei ist 64x32, die Vorderseite liegt bei (1,1) und ist
     * 10x16 gross. Das ganze Blatt anzuzeigen war der Fehler von frueher:
     * dann steht ein winziges Bild in einer leeren Flaeche. Hoehere
     * Aufloesungen sind Vielfache davon, deshalb der Faktor aus der Breite.
     */
    @Composable
    private fun Vorschau(png: ByteArray?, modifier: Modifier) {
        val theme = LocalTheme.current
        val bild = remember(png) {
            png?.let { runCatching { loadImageBitmap(ByteArrayInputStream(it)) }.getOrNull() }
        }
        if (bild == null) {
            Box(modifier.clip(theme.checkBoxShape).background(theme.componentBackground))
            return
        }
        val f = (bild.width / 64).coerceAtLeast(1)
        Image(
            painter = BitmapPainter(
                bild,
                IntOffset(1 * f, 1 * f),
                IntSize(10 * f, 16 * f)
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(theme.checkBoxShape)
        )
    }
}
