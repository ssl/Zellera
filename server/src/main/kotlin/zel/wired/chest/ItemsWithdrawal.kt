package zel.wired.chest

data class ItemsWithdrawal(
    val chestId: Int,
    val wall: Boolean,
    val typeId: Int,
    val extra: String,
    val quantity: Int,
)
