package zel.wired.chest

data class TransactionRow(
    val id: Long,
    val roomId: Int,
    val chestId: Int,
    val coins: Boolean,
    val type: Int,
    val definition: String,
    val userId: Int,
    val userName: String,
    val withdrawn: Int,
    val deposited: Int,
    val timestamp: Int,
    val furniIn: List<DetailLine> = emptyList(),
    val furniOut: List<DetailLine> = emptyList(),
) {
    companion object {
        const val MANUAL = 0
        const val WIRED = 1
        const val PAYMENT = 2
        const val REWARD = 3
        const val TRADE = 4
        const val AUTO_WITHDRAW = 5
    }
}
