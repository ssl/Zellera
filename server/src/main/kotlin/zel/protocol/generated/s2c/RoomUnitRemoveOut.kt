package zel.protocol.generated

data class RoomUnitRemoveOut(
    val string1: String,
)

object RoomUnitRemoveOutCodec : OutgoingCodec<RoomUnitRemoveOut> {
    override val name = "RoomUnitRemoveOut"

    override fun read(r: PacketReader): RoomUnitRemoveOut = readRoomUnitRemoveOut(r)

    override fun write(w: PacketWriter, m: RoomUnitRemoveOut) = writeRoomUnitRemoveOut(w, m)

    private fun readRoomUnitRemoveOut(r: PacketReader): RoomUnitRemoveOut {
        val vString1 = r.string()
        return RoomUnitRemoveOut(vString1)
    }

    private fun writeRoomUnitRemoveOut(w: PacketWriter, m: RoomUnitRemoveOut) {
        w.string(m.string1)
    }
}
