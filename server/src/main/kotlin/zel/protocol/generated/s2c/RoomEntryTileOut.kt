package zel.protocol.generated

data class RoomEntryTileOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object RoomEntryTileOutCodec : OutgoingCodec<RoomEntryTileOut> {
    override val name = "RoomEntryTileOut"

    override fun read(r: PacketReader): RoomEntryTileOut = readRoomEntryTileOut(r)

    override fun write(w: PacketWriter, m: RoomEntryTileOut) = writeRoomEntryTileOut(w, m)

    private fun readRoomEntryTileOut(r: PacketReader): RoomEntryTileOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return RoomEntryTileOut(vInt1, vInt2, vInt3)
    }

    private fun writeRoomEntryTileOut(w: PacketWriter, m: RoomEntryTileOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
