package zel.protocol.generated

data object ForwardToASubmittableRoomIn

object ForwardToASubmittableRoomInCodec : IncomingCodec<ForwardToASubmittableRoomIn> {
    override val name = "ForwardToASubmittableRoomIn"

    override fun read(r: PacketReader): ForwardToASubmittableRoomIn = ForwardToASubmittableRoomIn

    override fun write(w: PacketWriter, m: ForwardToASubmittableRoomIn) = writeForwardToASubmittableRoomIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeForwardToASubmittableRoomIn(w: PacketWriter, m: ForwardToASubmittableRoomIn) {
    }
}
