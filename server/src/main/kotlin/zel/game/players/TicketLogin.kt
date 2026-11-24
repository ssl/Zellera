package zel.game.players

sealed interface TicketLogin {
    /** [device] is the reading the check passed; null with the check off. */
    class Admitted(
        val player: Player,
        val device: DeviceReading? = null,
    ) : TicketLogin

    /** No live ticket like it, or its player could not be loaded. */
    data object Unknown : TicketLogin

    /** The device check turned it away; [differing] names the pieces that are not the same. */
    class Refused(
        val reason: Reason,
        val differing: List<String> = emptyList(),
    ) : TicketLogin

    enum class Reason {
        NO_DEVICE,
        DEVICE_MISMATCH,
    }
}
