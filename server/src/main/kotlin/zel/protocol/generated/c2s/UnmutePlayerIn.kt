package zel.protocol.generated

data class UnmutePlayerIn(
    val userId: Int,
    val roomId: Int,
)

object UnmutePlayerInCodec : IncomingCodec<UnmutePlayerIn> {
    override val name = "UnmutePlayerIn"

    override fun read(r: PacketReader): UnmutePlayerIn = readUnmutePlayerIn(r)

    override fun write(w: PacketWriter, m: UnmutePlayerIn) = writeUnmutePlayerIn(w, m)

    private fun readUnmutePlayerIn(r: PacketReader): UnmutePlayerIn {
        val vUserId = r.int()
        val vRoomId = r.int()
        return UnmutePlayerIn(vUserId, vRoomId)
    }

    private fun writeUnmutePlayerIn(w: PacketWriter, m: UnmutePlayerIn) {
        w.int(m.userId)
        w.int(m.roomId)
    }
}
