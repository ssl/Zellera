package zel.game.rewardtracks

@Suppress("MagicNumber") // the result numbers the client reads
enum class ClaimResult(
    val code: Int,
) {
    OK(0),
    TRACK_NOT_FOUND(2),
    REWARD_NOT_FOUND(3),
    NOT_ENOUGH_POINTS(5),
    ALREADY_CLAIMED(6),
    FAILED(7),
    PREMIUM_REQUIRED(8),
}
