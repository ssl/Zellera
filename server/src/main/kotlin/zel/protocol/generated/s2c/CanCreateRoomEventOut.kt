package zel.protocol.generated

data class CanCreateRoomEventOut(
    val canCreate: Boolean,
    val errorCode: Int,
)

object CanCreateRoomEventOutCodec : OutgoingCodec<CanCreateRoomEventOut> {
    override val name = "CanCreateRoomEventOut"

    override fun read(r: PacketReader): CanCreateRoomEventOut = readCanCreateRoomEventOut(r)

    override fun write(w: PacketWriter, m: CanCreateRoomEventOut) = writeCanCreateRoomEventOut(w, m)

    private fun readCanCreateRoomEventOut(r: PacketReader): CanCreateRoomEventOut {
        val vCanCreate = r.boolean()
        val vErrorCode = r.int()
        return CanCreateRoomEventOut(vCanCreate, vErrorCode)
    }

    private fun writeCanCreateRoomEventOut(w: PacketWriter, m: CanCreateRoomEventOut) {
        w.boolean(m.canCreate)
        w.int(m.errorCode)
    }
}
