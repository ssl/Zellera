package zel.protocol.generated

data class RoomPlayersClassificationIn(
    val string1: String,
)

object RoomPlayersClassificationInCodec : IncomingCodec<RoomPlayersClassificationIn> {
    override val name = "RoomPlayersClassificationIn"

    override fun read(r: PacketReader): RoomPlayersClassificationIn = readRoomPlayersClassificationIn(r)

    override fun write(w: PacketWriter, m: RoomPlayersClassificationIn) = writeRoomPlayersClassificationIn(w, m)

    private fun readRoomPlayersClassificationIn(r: PacketReader): RoomPlayersClassificationIn {
        val vString1 = r.string()
        return RoomPlayersClassificationIn(vString1)
    }

    private fun writeRoomPlayersClassificationIn(w: PacketWriter, m: RoomPlayersClassificationIn) {
        w.string(m.string1)
    }
}
