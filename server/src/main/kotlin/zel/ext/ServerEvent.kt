package zel.ext

sealed interface ServerEvent {
    val playerId: Int

    data class LoggedIn(
        override val playerId: Int,
    ) : ServerEvent

    data class LoggedOut(
        override val playerId: Int,
    ) : ServerEvent

    data class EnteredRoom(
        override val playerId: Int,
        val roomId: Int,
    ) : ServerEvent

    data class LeftRoom(
        override val playerId: Int,
        val roomId: Int,
    ) : ServerEvent

    data class Purchased(
        override val playerId: Int,
        val offerId: Int,
        val amount: Int,
    ) : ServerEvent
}
