package zel.protocol.generated

data class UnblockPlayerIn(
    val userId: Int,
)

object UnblockPlayerInCodec : IncomingCodec<UnblockPlayerIn> {
    override val name = "UnblockPlayerIn"

    override fun read(r: PacketReader): UnblockPlayerIn = readUnblockPlayerIn(r)

    override fun write(w: PacketWriter, m: UnblockPlayerIn) = writeUnblockPlayerIn(w, m)

    private fun readUnblockPlayerIn(r: PacketReader): UnblockPlayerIn {
        val vUserId = r.int()
        return UnblockPlayerIn(vUserId)
    }

    private fun writeUnblockPlayerIn(w: PacketWriter, m: UnblockPlayerIn) {
        w.int(m.userId)
    }
}
