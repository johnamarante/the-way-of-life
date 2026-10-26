package com.thewayoflife.rosary.game

class RosaryPosition(
    val beadCount: Int,
    initialPosition: Int = 0,
) {
    init {
        require(beadCount > 0)
        require(initialPosition in 0 until beadCount)
    }

    var current: Int = initialPosition
        private set

    val litIndexes: IntRange
        get() = 0..current

    fun advance(): Int {
        current = (current + 1) % beadCount
        return current
    }

    fun retreat(): Int {
        current = if (current == 0) beadCount - 1 else current - 1
        return current
    }
}
