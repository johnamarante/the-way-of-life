package com.thewayoflife.rosary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.thewayoflife.rosary.ui.AboutPanel
import com.thewayoflife.rosary.ui.ChallengeModal
import com.thewayoflife.rosary.ui.CodexPanel
import com.thewayoflife.rosary.ui.ConfirmNewGamePanel
import com.thewayoflife.rosary.ui.GameBar
import com.thewayoflife.rosary.ui.GameViewModel
import com.thewayoflife.rosary.ui.MenuPanel
import com.thewayoflife.rosary.ui.OptionsPanel
import com.thewayoflife.rosary.ui.Palette
import com.thewayoflife.rosary.ui.RosaryBoard
import com.thewayoflife.rosary.ui.Screen
import com.thewayoflife.rosary.ui.TitleScreen
import com.thewayoflife.rosary.ui.VersionLabel
import com.thewayoflife.rosary.ui.VictoryPanel
import com.thewayoflife.rosary.ui.WayOfLifeTheme

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WayOfLifeTheme {
                WayOfLifeApp(vm, onQuit = { finish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.appInForeground = true
    }

    override fun onPause() {
        vm.appInForeground = false
        super.onPause()
    }
}

@Composable
private fun WayOfLifeApp(vm: GameViewModel, onQuit: () -> Unit) {
    val context = LocalContext.current
    val board = vm.data.board
    BackHandler(enabled = vm.screen != Screen.MENU && vm.screen != Screen.TITLE) {
        if (vm.screen == Screen.GAME) vm.leaveGame() else vm.show(Screen.MENU)
    }
    // Text size from Options scales every panel and challenge (spec 11.2).
    val density = LocalDensity.current
    val scaledText = Density(density.density, density.fontScale * vm.options.textSize.scale)
    Box(Modifier.fillMaxSize().background(Palette.parchment)) {
        RosaryBoard(board.beads, board.cross, vm.litUpTo, vm.current, zoomed = vm.screen != Screen.TITLE, Modifier.fillMaxSize())
        if (vm.screen == Screen.TITLE) TitleScreen(onOpen = { vm.show(Screen.MENU) })
        if (vm.screen == Screen.GAME && vm.fullScreen) Box(Modifier.fillMaxSize().background(Palette.paper))
        CompositionLocalProvider(LocalDensity provides scaledText) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (vm.screen) {
                    Screen.TITLE -> Unit
                    Screen.GAME -> {
                        ChallengeModal(vm, Modifier.fillMaxSize())
                        if (!vm.fullScreen) {
                            GameBar(
                                vm,
                                onMenu = { vm.leaveGame() },
                                onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    }
                    Screen.MENU -> {
                        MenuPanel(vm, onQuit)
                        VersionLabel(Modifier.align(Alignment.BottomCenter))
                    }
                    Screen.CONFIRM_NEW_GAME -> ConfirmNewGamePanel(vm)
                    Screen.OPTIONS -> OptionsPanel(vm)
                    Screen.CODEX -> CodexPanel(vm)
                    Screen.ABOUT -> AboutPanel(vm)
                    Screen.VICTORY -> VictoryPanel(vm)
                }
            }
        }
    }
}
