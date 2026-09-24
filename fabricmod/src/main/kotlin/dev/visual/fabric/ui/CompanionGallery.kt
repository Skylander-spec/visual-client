package dev.visual.fabric.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.visual.fabric.Begleiter
import dev.visual.fabric.VConfig
import net.minecraft.client.resource.language.I18n
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** Illustrated animated cards; selection is also shown in the live Minecraft mirror. */
@Composable
fun CompanionGallery(e: Dp, done: () -> Unit) {
    var selected by remember { mutableIntStateOf(VConfig.get().miniMeArt) }
    val transition = rememberInfiniteTransition()
    val clock by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)))
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(e * .3f)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(VText.t("pet.choose"), color = LocalTheme.current.textColor,
                fontSize = (e.value * .43f).sp, modifier = Modifier.weight(1f))
            Text(VText.t("ui.done"), color = LocalTheme.current.textColor,
                modifier = Modifier.onClick(rememberInteractionSource(), done).padding(e * .3f))
        }
        Text(VText.t("pet.previewhint"), color = LocalTheme.current.textColorSecondary,
            fontSize = (e.value * .28f).sp)
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(e * .25f),
            verticalArrangement = Arrangement.spacedBy(e * .25f)) {
            items(Begleiter.NAMEN.size) { kind ->
                val shape = RoundedCornerShape(e * .25f)
                Column(Modifier.fillMaxWidth().background(if (selected == kind) Color(0xFF253D50) else Color(0xFF18232D), shape)
                    .border(if (selected == kind) 2.dp else 1.dp, if (selected == kind) Color(0xFFABDFFF) else Color(0xFF405264), shape)
                    .onClick(rememberInteractionSource()) {
                        selected = kind; VConfig.get().miniMeArt = kind; VConfig.get().miniMe = true; VConfig.save()
                    }.padding(e * .22f), horizontalAlignment = Alignment.CenterHorizontally) {
                    PetPortrait(kind, clock, Modifier.fillMaxWidth().height(e * 2.2f))
                    Text(I18n.translate(if (kind == 0) "vc.val.ownskin" else Begleiter.NAMEN[kind]),
                        color = Color.White, fontSize = (e.value * .28f).sp)
                    Text(VText.t(if (Begleiter.flying(kind)) "pet.fly" else "pet.sitwalk"),
                        color = Color(0xFFA9BDC9), fontSize = (e.value * .23f).sp)
                }
            }
        }
    }
}

@Composable
private fun PetPortrait(kind: Int, clock: Float, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.height / 50f
        val bob = kotlin.math.sin(clock * 6.283f + kind) * if (Begleiter.flying(kind)) 3f else .7f
        val ox = size.width / 2 - 16*u
        val oy = (4+bob)*u
        val fur = when(kind) {
            0 -> Color(0xFFD4A786); 3,10 -> Color(0xFFD8753C); 7 -> Color(0xFFBFA0FF)
            8,13 -> Color(0xFF9BE8F3); 9 -> Color(0xFFF5F3E8); 11 -> Color(0xFF24313B)
            14 -> Color(0xFFFFD976); 15 -> Color(0xFFF6AAC8); 16 -> Color(0xFF83BE89)
            else -> Color(0xFFD1D5E0)
        }
        fun rect(x:Float,y:Float,w:Float,h:Float,color:Color,r:Float=2f) =
            drawRoundRect(color,Offset(ox+x*u,oy+y*u),Size(w*u,h*u),CornerRadius(r*u,r*u))
        if (Begleiter.flying(kind)) {
            rect(-4f,22f,10f,6f,Color(0xFFDBF7FF));rect(26f,22f,10f,6f,Color(0xFFDBF7FF))
        }
        rect(7f,23f,18f,16f,if(kind==0) Color(0xFF90B9E5) else fur)
        rect(5f,37f,8f,6f,fur);rect(19f,37f,8f,6f,fur)
        if (kind!=0 && kind!=13) {
            val ear=if(kind==9) Color(0xFF27313A) else fur
            rect(2f,if(kind==4||kind==12) -1f else 4f,7f,if(kind==4||kind==12) 15f else 8f,ear)
            rect(23f,if(kind==4||kind==12) -1f else 4f,7f,if(kind==4||kind==12) 15f else 8f,ear)
        }
        rect(1f,9f,30f,23f,fur,5f)
        if(kind==0) rect(1f,8f,30f,7f,Color(0xFF574136))
        if(kind==9) {rect(5f,16f,9f,10f,Color(0xFF27313A));rect(18f,16f,9f,10f,Color(0xFF27313A))}
        val blink=(clock + kind*.071f)%1f > .94f
        val eye=if(kind==9||kind==11) Color.White else Color(0xFF27313A)
        rect(8f,20f,3f,if(blink) 1f else 4f,eye,.7f);rect(21f,20f,3f,if(blink) 1f else 4f,eye,.7f)
        rect(14f,26f,4f,2f,Color(0xFFCA8296),1f)
        rect(4f,25f,4f,2f,Color(0xFFF7B2C8),1f);rect(25f,25f,4f,2f,Color(0xFFF7B2C8),1f)
    }
}
