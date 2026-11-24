package zel.game.rewardtracks

@Suppress("MagicNumber") // the result numbers the client reads
enum class PassPurchase(
    val code: Int,
) {
    OK(0),
    TRACK_NOT_FOUND(2),
    LANE_IS_EVERYONES(3),
    NOT_ON_SALE(4),
    ALREADY_OWNED(5),
    NOT_ENOUGH_CREDITS(7),
    NOT_ENOUGH_PREMIUM(8),
    FAILED(9),
}
