package zel.room.persist

data class FurniRow(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val definitionId: Int,
    val wallPosition: String,
    val x: Int,
    val y: Int,
    val z: Double,
    val direction: Int,
    val state: String,
    /** "edition size:serial"; "0:0" when not limited. */
    val limitedData: String,
    val groupId: Int,
    val clientId: Int? = null,
    val wiredCopy: Boolean = false,
)
