package zel.protocol.generated

data class RoomEntryInfoOut(
    val int1: Int,
    val boolean1: Boolean,
)

object RoomEntryInfoOutCodec : OutgoingCodec<RoomEntryInfoOut> {
    override val name = "RoomEntryInfoOut"

    override fun read(r: PacketReader): RoomEntryInfoOut = readRoomEntryInfoOut(r)

    override fun write(w: PacketWriter, m: RoomEntryInfoOut) = writeRoomEntryInfoOut(w, m)

    private fun readRoomEntryInfoOut(r: PacketReader): RoomEntryInfoOut {
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        return RoomEntryInfoOut(vInt1, vBoolean1)
    }

    private fun writeRoomEntryInfoOut(w: PacketWriter, m: RoomEntryInfoOut) {
        w.int(m.int1)
        w.boolean(m.boolean1)
    }
}
