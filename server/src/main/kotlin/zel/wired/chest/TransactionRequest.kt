package zel.wired.chest

data class TransactionRequest(
    val playerId: Int,
    val unitIndex: Int,
    val contractId: Int,
    val contract: Contract,
    val mode: Int,
    val multiplier: Int,
    val chestIds: List<Int>,
    val timeoutSeconds: Int,
)
