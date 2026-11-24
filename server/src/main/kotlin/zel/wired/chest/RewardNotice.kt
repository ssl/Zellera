package zel.wired.chest

data class RewardNotice(
    val category: Int = Contract.CATEGORY_GAMES,
    val popup: Boolean = true,
    val text: String = "",
)
