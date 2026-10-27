package com.thewayoflife.rosary.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.thewayoflife.rosary.data.Spot
import com.thewayoflife.rosary.game.Rules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val ZOOM = 1.8f
private val MOVE = tween<Float>(700)

/**
 * The rosary board with lit beads 0..[litUpTo] and a brighter glow on [current] (the Cross is index 58).
 * When [zoomed], the view is enlarged and follows the current bead (spec 1.3); otherwise the whole board shows.
 */
@Composable
fun RosaryBoard(beads: List<Spot>, cross: Spot, litUpTo: Int, current: Int, zoomed: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("rosary_board.jpg").use(BitmapFactory::decodeStream)?.asImageBitmap()
        }
    }
    val spots = remember(beads, cross) { beads + cross }
    val glow = remember(spots.size) { List(spots.size) { Animatable(0f) } }
    LaunchedEffect(litUpTo) {
        coroutineScope {
            glow.forEachIndexed { index, level ->
                launch { level.animateTo(if (index <= litUpTo) 1f else 0f, tween(200)) }
            }
        }
    }

    // During the penance fall there is briefly no current bead; follow the lit beads down instead.
    val focus = spots[minOf(if (current >= 0) current else litUpTo, Rules.CROSS)]
    val scale by animateFloatAsState(if (zoomed) ZOOM else 1f, MOVE, label = "zoom")
    val focusX by animateFloatAsState(focus.x, MOVE, label = "focusX")
    val focusY by animateFloatAsState(focus.y, MOVE, label = "focusY")
    // Challenges open at the top of the screen when the bead is in the lower half of the board, so put
    // the bead in the part of the screen the challenge leaves open.
    val screenY by animateFloatAsState(if (focus.y > 0.5f) 0.72f else 0.3f, MOVE, label = "screenY")

    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val bitmap = image ?: return@Canvas
        val imageAspect = bitmap.width.toFloat() / bitmap.height
        val fitWidth = minOf(size.width, size.height * imageAspect)
        val drawnWidth = fitWidth * scale
        val drawnHeight = drawnWidth / imageAspect
        // Centered when smaller than the screen; otherwise as close to the focus as the board's edges allow.
        fun place(drawn: Float, view: Float, wanted: Float) = if (drawn <= view) (view - drawn) / 2f else wanted.coerceIn(view - drawn, 0f)
        val left = place(drawnWidth, size.width, size.width / 2f - focusX * drawnWidth)
        val top = place(drawnHeight, size.height, size.height * screenY - focusY * drawnHeight)
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt()),
            filterQuality = FilterQuality.High,
        )
        spots.forEachIndexed { index, spot ->
            val alpha = glow[index].value
            if (alpha > 0f) {
                val isCurrent = index == current || (index == Rules.CROSS && current >= Rules.CROSS)
                drawGlow(spot, alpha, isCurrent, left, top, drawnWidth, drawnHeight)
            }
        }
    }
}

private fun DrawScope.drawGlow(spot: Spot, alpha: Float, isCurrent: Boolean, left: Float, top: Float, width: Float, height: Float) {
    val center = Offset(left + spot.x * width, top + spot.y * height)
    val radius = spot.r * width * if (isCurrent) 2.25f else 1.8f
    val strength = alpha * if (isCurrent) 0.88f else 0.68f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF6D8).copy(alpha = strength),
                Color(0xFFFFE9A6).copy(alpha = strength * 0.38f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen,
    )
}

private val portraitCache = LruCache<String, ImageBitmap>(48)

/** A saint portrait from assets/portraits, decoded off the main thread and cached. */
@Composable
fun rememberPortrait(fileName: String): ImageBitmap? {
    val context = LocalContext.current
    val portrait by produceState(initialValue = portraitCache.get(fileName), fileName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open("portraits/$fileName").use(BitmapFactory::decodeStream)?.asImageBitmap()
                }.getOrNull()
            }?.also { portraitCache.put(fileName, it) }
        }
    }
    return portrait
}
