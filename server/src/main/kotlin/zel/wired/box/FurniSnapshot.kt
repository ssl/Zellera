package zel.wired.box

data class FurniSnapshot(
    val furniId: Int,
    val state: String,
    val direction: Int,
    val x: Int,
    val y: Int,
    val z: Double,
    val wall: String? = null,
)
