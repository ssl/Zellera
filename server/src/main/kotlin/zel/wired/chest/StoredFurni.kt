package zel.wired.chest

data class StoredFurni(
    val slotId: Int,
    val transactionId: Long,
    val isWall: Boolean,
    val definitionId: Int,
    val spriteId: Int,
    val posterCode: String,
    val groupable: Boolean,
    val state: String,
    val limitedNumber: Int,
    val limitedTotal: Int,
    val extra: Int,
    val furniId: Int = 0,
)
