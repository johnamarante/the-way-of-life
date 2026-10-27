package com.thewayoflife.rosary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val parchment = Color(0xFFEFE3C2)
    val paper = Color(0xFFFBF5E6)
    val ink = Color(0xFF3B2A14)
    val inkSoft = Color(0xFF6B5534)
    val line = Color(0xFFB89B66)
    val gold = Color(0xFFB8860B)
    val goldSoft = Color(0xFFF3E2B3)
    val button = Color(0xFFFFFDF6)
    val right = Color(0xFF2F6B2F)
    val rightBg = Color(0xFFDCEBD5)
    val wrong = Color(0xFF8E2323)
    val wrongBg = Color(0xFFF3D9D4)
    val note = Color(0xFFFFF8E2)
}

/** Times New Roman is not on Android; the system serif font is the closest match (spec 1.3). */
private val Serif = FontFamily.Serif

private fun TextStyle.serif() = copy(fontFamily = Serif, color = Palette.ink)

private val SerifTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.serif(), displayMedium = displayMedium.serif(), displaySmall = displaySmall.serif(),
        headlineLarge = headlineLarge.serif(), headlineMedium = headlineMedium.serif(), headlineSmall = headlineSmall.serif(),
        titleLarge = titleLarge.serif(), titleMedium = titleMedium.serif(), titleSmall = titleSmall.serif(),
        bodyLarge = bodyLarge.serif().copy(fontSize = 18.sp, lineHeight = 25.sp),
        bodyMedium = bodyMedium.serif().copy(fontSize = 17.sp, lineHeight = 23.sp),
        bodySmall = bodySmall.serif(),
        labelLarge = labelLarge.serif(), labelMedium = labelMedium.serif(), labelSmall = labelSmall.serif(),
    )
}

@Composable
fun WayOfLifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(typography = SerifTypography, content = content)
}

enum class ButtonStyle { NORMAL, PRIMARY, RIGHT, WRONG, SELECTED }

/** The game's plain, bordered button; also used for prayer capsules with a pill [shape]. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.NORMAL,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    fontSize: Int = 18,
) {
    val (background, border, textColor) = when (style) {
        ButtonStyle.NORMAL -> Triple(Palette.button, Palette.line, Palette.ink)
        ButtonStyle.PRIMARY -> Triple(Palette.gold, Color(0xFF8C6508), Palette.button)
        ButtonStyle.RIGHT -> Triple(Palette.rightBg, Palette.right, Palette.right)
        ButtonStyle.WRONG -> Triple(Palette.wrongBg, Palette.wrong, Palette.wrong)
        ButtonStyle.SELECTED -> Triple(Palette.goldSoft, Palette.gold, Palette.ink)
    }
    val dim = !enabled && (style == ButtonStyle.NORMAL || style == ButtonStyle.PRIMARY)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dim) 0.5f else 1f)
            .clip(shape)
            .background(background)
            .border(BorderStroke(if (style == ButtonStyle.SELECTED) 2.dp else 1.dp, border), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = fontSize.sp, textAlign = TextAlign.Center, lineHeight = (fontSize + 5).sp)
    }
}
