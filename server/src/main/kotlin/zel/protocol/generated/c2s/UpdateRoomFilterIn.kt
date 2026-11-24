package zel.protocol.generated

data class UpdateRoomFilterIn(
    val roomId: Int,
    val add: Boolean,
    val word: String,
)

object UpdateRoomFilterInCodec : IncomingCodec<UpdateRoomFilterIn> {
    override val name = "UpdateRoomFilterIn"

    override fun read(r: PacketReader): UpdateRoomFilterIn = readUpdateRoomFilterIn(r)

    override fun write(w: PacketWriter, m: UpdateRoomFilterIn) = writeUpdateRoomFilterIn(w, m)

    private fun readUpdateRoomFilterIn(r: PacketReader): UpdateRoomFilterIn {
        val vRoomId = r.int()
        val vAdd = r.boolean()
        val vWord = r.string()
        return UpdateRoomFilterIn(vRoomId, vAdd, vWord)
    }

    private fun writeUpdateRoomFilterIn(w: PacketWriter, m: UpdateRoomFilterIn) {
        w.int(m.roomId)
        w.boolean(m.add)
        w.string(m.word)
    }
}
