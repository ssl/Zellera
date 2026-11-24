package zel.wired.chest

import zel.protocol.generated.StuffData

data class OfferedFurni(
    val id: Int,
    val definitionId: Int,
    val spriteId: Int,
    val wall: Boolean,
    val name: String,
    val state: String,
    val limitedTotal: Int,
    val limitedNumber: Int,
    val tradable: Boolean,
    val stuff: StuffData,
    val category: Int,
    val extra: Int,
)
