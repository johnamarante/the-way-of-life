package com.thewayoflife.rosary

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thewayoflife.rosary.game.RosaryViewModel
import com.thewayoflife.rosary.model.Bead
import com.thewayoflife.rosary.model.BoardLayout
import com.thewayoflife.rosary.model.BoardLayoutLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val Parchment = Color(0xFFEFE3C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                RosaryApp()
            }
        }
    }
}

@Composable
private fun RosaryApp(viewModel: RosaryViewModel = viewModel()) {
    val context = LocalContext.current
    val boardData by produceState<Pair<BoardLayout, ImageBitmap>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            val layout = BoardLayoutLoader(context.assets).load()
            val bitmap = context.assets.open("rosary_board.jpg").use(BitmapFactory::decodeStream)
                ?: error("Unable to decode rosary board image")
            layout to bitmap.asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
    ) {
        boardData?.let { (layout, image) ->
            RosaryBoard(
                layout = layout,
                image = image,
                currentPosition = viewModel.currentPosition,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            ArrowButton("←", "Back one bead", viewModel::retreat)
            ArrowButton("→", "Forward one bead", viewModel::advance)
        }
    }
}

@Composable
private fun ArrowButton(label: String, description: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0x995B4026),
            contentColor = Color.White,
        ),
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun RosaryBoard(
    layout: BoardLayout,
    image: ImageBitmap,
    currentPosition: Int,
    modifier: Modifier = Modifier,
) {
    val glowLevels = remember(layout.beads.size) {
        List(layout.beads.size) { index -> Animatable(if (index == 0) 1f else 0f) }
    }

    LaunchedEffect(currentPosition) {
        coroutineScope {
            glowLevels.forEachIndexed { index, level ->
                launch {
                    level.animateTo(
                        targetValue = if (index <= currentPosition) 1f else 0f,
                        animationSpec = tween(durationMillis = 200),
                    )
                }
            }
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val imageAspect = image.width.toFloat() / image.height
        val canvasAspect = size.width / size.height
        val drawnWidth: Float
        val drawnHeight: Float
        if (canvasAspect > imageAspect) {
            drawnHeight = size.height
            drawnWidth = drawnHeight * imageAspect
        } else {
            drawnWidth = size.width
            drawnHeight = drawnWidth / imageAspect
        }
        val imageLeft = (size.width - drawnWidth) / 2f
        val imageTop = (size.height - drawnHeight) / 2f

        drawImage(
            image = image,
            dstOffset = IntOffset(imageLeft.roundToInt(), imageTop.roundToInt()),
            dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt()),
        )

        layout.beads.forEachIndexed { index, bead ->
            val alpha = glowLevels[index].value
            if (alpha > 0f) {
                drawGlow(
                    bead = bead,
                    alpha = alpha,
                    isCurrent = index == currentPosition,
                    imageLeft = imageLeft,
                    imageTop = imageTop,
                    drawnWidth = drawnWidth,
                    drawnHeight = drawnHeight,
                )
            }
        }
    }
}

private fun DrawScope.drawGlow(
    bead: Bead,
    alpha: Float,
    isCurrent: Boolean,
    imageLeft: Float,
    imageTop: Float,
    drawnWidth: Float,
    drawnHeight: Float,
) {
    val center = Offset(
        x = imageLeft + bead.x * drawnWidth,
        y = imageTop + bead.y * drawnHeight,
    )
    val beadRadius = bead.radius * drawnWidth
    val radius = beadRadius * if (isCurrent) 2.25f else 1.8f
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
