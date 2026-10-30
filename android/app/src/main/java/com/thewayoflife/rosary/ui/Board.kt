package com.thewayoflife.rosary.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
    // Bursts on beads that just lit up: each one on a short step (a Latin skip lights six at once), only the
    // new bead on a long jump (restored after penance), none on the first composition or when beads go out.
    val bursts = remember { mutableStateListOf<Burst>() }
    val shownLit = remember { intArrayOf(litUpTo) }
    val burstScope = rememberCoroutineScope()
    LaunchedEffect(litUpTo) {
        val from = shownLit[0]
        shownLit[0] = litUpTo
        if (litUpTo <= from) return@LaunchedEffect
        for (index in (if (litUpTo - from <= 6) from + 1 else litUpTo)..minOf(litUpTo, Rules.CROSS)) {
            val burst = Burst(index)
            bursts.add(burst)
            burstScope.launch {
                burst.progress.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
                bursts.remove(burst)
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
        bursts.forEach { drawBurst(it, spots[it.index], left, top, drawnWidth, drawnHeight) }
    }
}

private const val BURST_MS = 1300
private const val SPARKS = 22

/** One spark of a [Burst]: its direction, how far it flies and its size (in bead radii), and its share of the burst's time. */
private class Spark(val angle: Float, val distance: Float, val size: Float, val life: Float)

/** A white sunburst from a bead that has just lit up: a flash and sparks flying outward, fading to the lit bead. */
private class Burst(val index: Int) {
    val progress = Animatable(0f)
    val sparks = List(SPARKS) { i ->
        Spark(
            angle = ((i + Random.nextFloat() * 0.6f) / SPARKS * 2 * PI).toFloat(),
            distance = 5f + Random.nextFloat() * 3f,
            size = 0.2f + Random.nextFloat() * 0.15f,
            life = 0.75f + Random.nextFloat() * 0.25f,
        )
    }
}

private fun DrawScope.drawBurst(burst: Burst, spot: Spot, left: Float, top: Float, width: Float, height: Float) {
    val center = Offset(left + spot.x * width, top + spot.y * height)
    val r = spot.r * width
    val p = burst.progress.value
    // The flash: a white glow that grows from the bead and fades in the first half of the burst.
    val flash = (p / 0.5f).coerceAtMost(1f)
    if (flash < 1f) {
        val radius = r * 4.5f * (0.2f + 0.8f * (1f - (1f - flash) * (1f - flash)))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 1f - flash), Color.White.copy(alpha = 0.55f * (1f - flash)), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
    // The sparks fly out fast, slow down, drift down a little and fade.
    burst.sparks.forEach { spark ->
        val t = (p / spark.life).coerceAtMost(1f)
        if (t >= 1f) return@forEach
        val travel = 1f - (1f - t) * (1f - t) * (1f - t)
        val sparkCenter = Offset(
            center.x + cos(spark.angle) * spark.distance * r * travel,
            center.y + sin(spark.angle) * spark.distance * r * travel + r * 1.5f * t * t,
        )
        val alpha = if (t < 0.55f) 1f else 1f - (t - 0.55f) / 0.45f
        val size = maxOf(1.5f, spark.size * r * (1f - 0.6f * t))
        drawCircle(Color.White.copy(alpha = alpha * 0.35f), radius = size * 2.2f, center = sparkCenter)
        drawCircle(Color.White.copy(alpha = alpha), radius = size, center = sparkCenter)
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

/** A saint portrait from assets/portraits (or a picture from another assets [folder]), decoded off the main thread and cached. */
@Composable
fun rememberPortrait(fileName: String, folder: String = "portraits"): ImageBitmap? {
    val context = LocalContext.current
    val path = "$folder/$fileName"
    val portrait by produceState(initialValue = portraitCache.get(path), path) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(path).use(BitmapFactory::decodeStream)?.asImageBitmap()
                }.getOrNull()
            }?.also { portraitCache.put(path, it) }
        }
    }
    return portrait
}
