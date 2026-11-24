package zel.room.persist

data class FurniSave(
    val id: Int,
    val wallPosition: String,
    val x: Int,
    val y: Int,
    val z: Double,
    val direction: Int,
    val state: String,
)
