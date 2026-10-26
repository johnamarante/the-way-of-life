package com.thewayoflife.rosary.model

data class Bead(
    val index: Int,
    val type: BeadType,
    val x: Float,
    val y: Float,
    val radius: Float,
)

enum class BeadType {
    REGULAR,
    GLORY,
}

data class BoardLayout(
    val image: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val beads: List<Bead>,
) {
    init {
        require(imageWidth > 0 && imageHeight > 0)
        require(beads.isNotEmpty())
        require(beads.map(Bead::index) == beads.indices.toList()) {
            "Beads must have contiguous indexes beginning at zero"
        }
    }
}
