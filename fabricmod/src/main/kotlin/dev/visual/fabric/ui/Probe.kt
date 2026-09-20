package dev.visual.fabric.ui

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen

/**
 * Rauchprobe: laesst sich OneConfigs ComposeScreen erben?
 *
 * Daran haengt alles Weitere. Ihr ComposeScreen ist der Bildschirm, der
 * eine Compose-Oberflaeche in Minecraft haelt - wer ihn erbt, wird von
 * ihrem Renderer gezeichnet und sieht deshalb genau aus wie ihr Menue.
 *
 * Drei Dinge muessen dafuer stimmen, und jedes hat den Bau einzeln
 * gekippt: Kotlin 2.4.20 (ihre Jars tragen Metadaten 2.4.0), der
 * Compose-Compiler in derselben Nummer, und Loom 1.17.21 - unter 1.17.20
 * scheiterte der Remapper an genau diesen Metadaten, und ohne Remappen
 * steht in ihrem Jar net.minecraft.class_437, wo wir Yarn erwarten.
 *
 * Laesst sich das hier nicht mehr uebersetzen, ist der Zugang zu ihrem
 * Menue weg - und zwar bevor ein Bildschirm daran haengt.
 */
internal class Probe : ComposeScreen() {
    @Composable
    override fun compose() {
    }
}
