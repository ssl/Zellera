package zel.protocol.generated

data class RoomUnitsOut(
    val rest: ByteArray,
)

object RoomUnitsOutCodec : OutgoingCodec<RoomUnitsOut> {
    override val name = "RoomUnitsOut"

    override fun read(r: PacketReader): RoomUnitsOut = readRoomUnitsOut(r)

    override fun write(w: PacketWriter, m: RoomUnitsOut) = writeRoomUnitsOut(w, m)

    private fun readRoomUnitsOut(r: PacketReader): RoomUnitsOut {
        val vRest = r.rest()
        return RoomUnitsOut(vRest)
    }

    private fun writeRoomUnitsOut(w: PacketWriter, m: RoomUnitsOut) {
        w.bytes(m.rest)
    }
}
