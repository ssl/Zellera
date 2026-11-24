package zel.room.trade

sealed interface TradeOpening {
    data object Ignored : TradeOpening

    data object NeedsTradePass : TradeOpening

    data class Refused(
        val failure: Int,
        val partnerName: String = "",
    ) : TradeOpening

    data class Opens(
        val asker: Trade.Side,
        val partner: Trade.Side,
    ) : TradeOpening
}
