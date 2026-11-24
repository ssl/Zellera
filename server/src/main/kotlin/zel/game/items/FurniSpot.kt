package zel.game.items

data class FurniSpot(
    val wallPosition: String,
    val x: Short,
    val y: Short,
    val z: Double,
    val direction: Int,
) {
    companion object {
        val NOWHERE = FurniSpot("", 0, 0, 0.0, 0)
    }
}
