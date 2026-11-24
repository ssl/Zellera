package zel.game.players

class PlayerOfferPurchase(
    val offerId: Int,
    state: Int = 0,
    bought: Int = 0,
    lastBoughtAt: Int = 0,
) {
    var state: Int = state
        set(value) {
            field = value
            changed = true
        }

    var bought: Int = bought
        private set

    var lastBoughtAt: Int = lastBoughtAt
        private set

    var changed = false
        private set

    fun record(
        amount: Int,
        at: Int,
    ) {
        bought += amount
        lastBoughtAt = at
        changed = true
    }

    fun markSaved() {
        changed = false
    }
}
