package com.thewayoflife.rosary.game

import org.junit.Assert.assertEquals
import org.junit.Test

class RosaryPositionTest {
    @Test
    fun `starts at zero with only bead zero lit`() {
        val position = RosaryPosition(58)

        assertEquals(0, position.current)
        assertEquals(listOf(0), position.litIndexes.toList())
    }

    @Test
    fun `advance lights the next bead`() {
        val position = RosaryPosition(58)

        position.advance()

        assertEquals(1, position.current)
        assertEquals(listOf(0, 1), position.litIndexes.toList())
    }

    @Test
    fun `retreat turns off the current bead`() {
        val position = RosaryPosition(58)
        repeat(3) { position.advance() }

        position.retreat()

        assertEquals(2, position.current)
        assertEquals(listOf(0, 1, 2), position.litIndexes.toList())
    }

    @Test
    fun `retreat from zero wraps to last bead and lights all beads`() {
        val position = RosaryPosition(58)

        position.retreat()

        assertEquals(57, position.current)
        assertEquals((0..57).toList(), position.litIndexes.toList())
    }

    @Test
    fun `advance from last bead wraps to zero`() {
        val position = RosaryPosition(58, initialPosition = 57)

        position.advance()

        assertEquals(0, position.current)
        assertEquals(listOf(0), position.litIndexes.toList())
    }

    @Test
    fun `lit set follows every step in both directions`() {
        val position = RosaryPosition(4)

        assertEquals(listOf(0, 1), position.advance().let { position.litIndexes.toList() })
        assertEquals(listOf(0, 1, 2), position.advance().let { position.litIndexes.toList() })
        assertEquals(listOf(0, 1), position.retreat().let { position.litIndexes.toList() })
        assertEquals(listOf(0), position.retreat().let { position.litIndexes.toList() })
        assertEquals(listOf(0, 1, 2, 3), position.retreat().let { position.litIndexes.toList() })
    }
}
