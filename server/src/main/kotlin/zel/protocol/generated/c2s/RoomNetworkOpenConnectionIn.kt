package zel.protocol.generated

data class RoomNetworkOpenConnectionIn(
    val networkId: Int,
    val homeRoomId: Int,
)

object RoomNetworkOpenConnectionInCodec : IncomingCodec<RoomNetworkOpenConnectionIn> {
    override val name = "RoomNetworkOpenConnectionIn"

    override fun read(r: PacketReader): RoomNetworkOpenConnectionIn = readRoomNetworkOpenConnectionIn(r)

    override fun write(w: PacketWriter, m: RoomNetworkOpenConnectionIn) = writeRoomNetworkOpenConnectionIn(w, m)

    private fun readRoomNetworkOpenConnectionIn(r: PacketReader): RoomNetworkOpenConnectionIn {
        val vNetworkId = r.int()
        val vHomeRoomId = r.int()
        return RoomNetworkOpenConnectionIn(vNetworkId, vHomeRoomId)
    }

    private fun writeRoomNetworkOpenConnectionIn(w: PacketWriter, m: RoomNetworkOpenConnectionIn) {
        w.int(m.networkId)
        w.int(m.homeRoomId)
    }
}
