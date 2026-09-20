package dev.visual.fabric.ui

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * Rauchprobe fuer die Uebersetzung: greift auf OneConfigs eigenes Theme zu.
 *
 * Steht hier, weil an dieser Stelle drei Dinge gleichzeitig stimmen muessen
 * und jedes einzelne den Bau schon gekippt hat: Kotlin 2.4.20 (ihre Jars
 * tragen Metadaten 2.4.0, mit 2.2 bricht das Lesen ab), der Compose-Compiler
 * in derselben Nummer, und OneConfig als compileOnly statt modCompileOnly
 * (Looms Remapper scheitert an denselben Metadaten).
 *
 * Laesst sich das hier nicht mehr uebersetzen, ist der Zugang zu ihrem
 * Menue weg - und zwar bevor irgendein Bildschirm daran haengt.
 */
@Composable
fun themeVorhanden(): Boolean = LocalTheme.current != null
