package zel.wired.chest

sealed interface Deal {
    val chestIds: List<Int>

    data class Deposit(
        val chestId: Int,
    ) : Deal {
        override val chestIds: List<Int> get() = listOf(chestId)
    }

    data class Negotiated(
        val contractId: Int,
        val contract: Contract,
        val mode: Int,
        val multiplier: Int,
        override val chestIds: List<Int>,
        val chestSide: List<StoredFurni>,
        val timeoutSeconds: Int,
    ) : Deal
}
