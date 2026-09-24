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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
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
class VCosmeticsScreen @JvmOverloads constructor(private val openPicker: Boolean = false) : ComposeScreen() {
    private var picking by mutableStateOf(openPicker)

    /**
     * ESC fuehrt zurueck, nicht ins Nichts.
     *
     * Minecrafts Screen.close() macht setScreen(null). Im Spiel ist das
     * richtig - im Titelbildschirm gibt es dann aber gar keinen
     * Bildschirm mehr, und man haengt fest. Genau das ist passiert.
     */
    override fun close() {
        if (picking) { picking = false; return }
        val client = MinecraftClient.getInstance()
        if (client.world == null) {
            client.setScreen(VisualTitleScreen.oeffnen())
        } else {
            client.setScreen(null)
        }
    }

    private class Stueck(val name: String, val bild: androidx.compose.ui.graphics.ImageBitmap?)
    private var umhaenge by mutableStateOf<List<Stueck>>(emptyList())
    private var syncStatus by mutableStateOf(dev.visual.fabric.CosmeticsSync.status())
    private var laden by mutableStateOf(true)
    private var meldung by mutableStateOf("")
    private var gewaehlt by mutableStateOf(CapeManager.angelegt() ?: "")
    private var anlegen by mutableStateOf(false)
    private val spiegel = Spiegel()
    private val minime by lazy { ModuleRegistry.all(this).first { it.id == "minime" } }

    init {
        dev.visual.fabric.CapeLibrary.load().thenApplyAsync { entries ->
            entries.map { entry -> Stueck(entry.name(), runCatching {
                loadImageBitmap(ByteArrayInputStream(entry.png()))
            }.getOrNull()) }
        }.whenComplete { entries, error ->
            MinecraftClient.getInstance().execute {
                laden = false
                if (error != null) meldung = VText.t("ui.loadfailed")
                else umhaenge = entries
            }
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
        var rubrik by remember { mutableStateOf(if (openPicker) "minime" else "umhaenge") }


        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val e = maxHeight * 0.042f
            val form = RoundedCornerShape(e * 0.5f)

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.80f)
                    .fillMaxHeight(0.78f)
                    .clip(form)
                    .drawBehind {
                        // OneConfig's GL overlay is composited after Minecraft's 3D GUI.
                        // Leave a real transparent viewport so it cannot cover the model.
                        clipRect(left = e.toPx() * 0.5f, top = e.toPx() * 1.8f,
                            right = e.toPx() * 6.9f, bottom = size.height - e.toPx() * 1.7f,
                            clipOp = ClipOp.Difference) { drawRect(theme.pageBackground) }
                    }
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
                            "minime" to VText.t("mod.minime")
                        )) {
                            Chip(name, rubrik == schluessel, e) { rubrik = schluessel }
                        }
                    }
                    Text(VText.t(syncStatus), color = theme.textColorSecondary, fontSize = (e.value * 0.27f).sp)
                    if (meldung.isNotEmpty()) Text(meldung, color = theme.textColorSecondary)
                    when (rubrik) {
                        "minime" -> if (picking) CompanionGallery(e) { picking = false }
                            else ModuleOptions(minime, e, Modifier.fillMaxSize())
                        else -> if (laden) Hinweis(VText.t("ui.loading"), e)
                            else Raster(umhaenge, gewaehlt, e) { neu ->
                                if (!anlegen) {
                                    anlegen = true
                                    dev.visual.fabric.CapeLibrary.equip(neu.ifEmpty { null }).whenComplete { _, error ->
                                        MinecraftClient.getInstance().execute {
                                            anlegen = false
                                            if (error == null) { gewaehlt = neu; meldung = "" }
                                            else meldung = VText.t("ui.equipfailed")
                                        }
                                    }
                                }
                            }
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
            Text("Powered by OneConfig", color = theme.textColorSecondary, fontSize = (e.value * 0.24f).sp)
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
        syncStatus = dev.visual.fabric.CosmeticsSync.status()
        // Restore the incoming matrix around Compose, before drawing Minecraft's model.
        Compat.matrixAuf(ctx)
        try { super.render(ctx, mausX, mausY, delta) }
        finally { Compat.matrixZu(ctx) }
        Compat.foreground(ctx)
        val e = height * 0.042f
        val left = width * 0.10f + e * 0.5f
        val top = height * 0.11f + e * 2f
        val bottom = height * 0.89f - e * 1.7f
        ctx.fill(left.toInt(), top.toInt(), (left + e * 6.4f).toInt(), bottom.toInt(), 0xFF161F26.toInt())
        spiegel.zeichne(ctx, left.toInt(), top.toInt(), (left + e * 6.4f).toInt(),
            bottom.toInt(), mausX, mausY, delta)
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
            items(liste, key = { it.name }) { stueck ->
                Karte(stueck, stueck.name == gewaehlt, e) {
                    val neu = if (stueck.name == gewaehlt) "" else stueck.name
                    waehlen(neu)

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
            Text(name, color = if (aktiv) theme.pageBackground else theme.textColor, fontSize = (e.value * 0.32f).sp)
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
            Vorschau(stueck.bild, Modifier.weight(1f).aspectRatio(10f / 16f), e)
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
    private fun Vorschau(bild: androidx.compose.ui.graphics.ImageBitmap?, modifier: Modifier, e: Dp) {
        val theme = LocalTheme.current
        val form = RoundedCornerShape(e * 0.2f)
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
