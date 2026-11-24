package zel.protocol.generated

data object NoOwnedRoomsAlertOut

object NoOwnedRoomsAlertOutCodec : OutgoingCodec<NoOwnedRoomsAlertOut> {
    override val name = "NoOwnedRoomsAlertOut"

    override fun read(r: PacketReader): NoOwnedRoomsAlertOut = NoOwnedRoomsAlertOut

    override fun write(w: PacketWriter, m: NoOwnedRoomsAlertOut) = writeNoOwnedRoomsAlertOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeNoOwnedRoomsAlertOut(w: PacketWriter, m: NoOwnedRoomsAlertOut) {
    }
}
