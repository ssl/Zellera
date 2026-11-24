package zel.wired.chest

class ChestSettlement(
    val paid: List<Int>,
    val chests: Map<Int, ChestRecord>,
    val rows: List<TransactionRow>,
    val delivered: List<Delivery>,
    val payouts: List<Payout> = emptyList(),
    val payerId: Int = 0,
) {
    /** [later] after this one: a chest in both is saved as [later] leaves it. */
    operator fun plus(later: ChestSettlement): ChestSettlement {
        require(payerId == 0 || later.payerId == 0 || payerId == later.payerId) { "one settlement has one payer" }
        return ChestSettlement(
            paid + later.paid,
            chests + later.chests,
            rows + later.rows,
            delivered + later.delivered,
            payouts + later.payouts,
            if (payerId != 0) payerId else later.payerId,
        )
    }

    data class Delivery(
        val playerId: Int,
        val furni: List<StoredFurni>,
    )

    /** [operationId] is fixed before the first try so a retried write books it once. */
    data class Payout(
        val playerId: Int,
        val credits: Int,
        val category: Int,
        val operationId: String,
        val createdAt: Int,
        val announced: Boolean = false,
    )

    data class Written<T>(
        val created: List<Pair<Int, T>>,
        val purses: Map<Int, Int>,
    )
}
