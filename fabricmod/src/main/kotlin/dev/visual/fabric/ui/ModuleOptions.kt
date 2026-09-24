package dev.visual.fabric.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** Adapter from Visual's settings to the original OneConfig controls. */
@Composable
fun ModuleOptions(module: VModule, e: Dp, modifier: Modifier = Modifier) {
    var revision by remember(module) { mutableIntStateOf(0) }
    LaunchedEffect(module) { while (true) { kotlinx.coroutines.delay(500); revision++ } }
    Column(modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(e * 0.35f)) {
        @Suppress("UNUSED_VARIABLE") val current = revision
        Text(module.title, color = LocalTheme.current.textColor, fontSize = (e.value * 0.48f).sp)
        Text(module.description, color = LocalTheme.current.textColorSecondary, fontSize = (e.value * 0.30f).sp)
        if (module.enabled != null && module.setEnabled != null) {
            SwitchControl(module.enabled.asBoolean) { module.setEnabled.accept(it); revision++ }
        }
        for (setting in module.settings) {
            Row(Modifier.fillMaxWidth().padding(vertical = e * 0.15f)
                .then(if (setting is VSetting.Action) Modifier.onClick(rememberInteractionSource()) { setting.run.run(); revision++ } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(e * 0.3f)) {
                // Read in each row: Compose may skip an unchanged Row lambda even
                // when the parent refreshed. Java suppliers are not snapshot state.
                @Suppress("UNUSED_VARIABLE") val current = revision
                Text(setting.label, color = LocalTheme.current.textColor,
                    fontSize = (e.value * 0.32f).sp, modifier = Modifier.weight(1f))
                when (setting) {
                    is VSetting.Toggle -> SwitchControl(setting.get.asBoolean) {
                        setting.set.accept(it); revision++
                    }
                    is VSetting.Stepper -> {
                        IconButton("/assets/visualsfabric/ico/arrow-left.svg", Modifier.size(e * 0.65f)) {
                            setting.prev.run(); revision++
                        }
                        Text(setting.display.get(), color = LocalTheme.current.textColor,
                            fontSize = (e.value * 0.32f).sp)
                        IconButton("/assets/visualsfabric/ico/chevron-right.svg", Modifier.size(e * 0.65f)) {
                            setting.next.run(); revision++
                        }
                    }
                    is VSetting.Info -> Text(setting.value.get(), color = LocalTheme.current.textColorSecondary,
                        fontSize = (e.value * 0.32f).sp)
                    is VSetting.Action -> IconButton("/assets/visualsfabric/ico/refresh-ccw-02.svg",
                        Modifier.size(e * 0.65f)) { setting.run.run(); revision++ }
                }
            }
        }
    }
}
