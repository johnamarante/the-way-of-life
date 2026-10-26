package com.thewayoflife.rosary.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RosaryViewModel : ViewModel() {
    private val position = RosaryPosition(BEAD_COUNT)

    var currentPosition by mutableIntStateOf(position.current)
        private set

    fun advance() {
        currentPosition = position.advance()
    }

    fun retreat() {
        currentPosition = position.retreat()
    }

    private companion object {
        const val BEAD_COUNT = 58
    }
}
