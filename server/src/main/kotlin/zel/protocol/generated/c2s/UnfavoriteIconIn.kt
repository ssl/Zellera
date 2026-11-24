package zel.protocol.generated

data class UnfavoriteIconIn(
    val id: Int,
)

object UnfavoriteIconInCodec : IncomingCodec<UnfavoriteIconIn> {
    override val name = "UnfavoriteIconIn"

    override fun read(r: PacketReader): UnfavoriteIconIn = readUnfavoriteIconIn(r)

    override fun write(w: PacketWriter, m: UnfavoriteIconIn) = writeUnfavoriteIconIn(w, m)

    private fun readUnfavoriteIconIn(r: PacketReader): UnfavoriteIconIn {
        val vId = r.int()
        return UnfavoriteIconIn(vId)
    }

    private fun writeUnfavoriteIconIn(w: PacketWriter, m: UnfavoriteIconIn) {
        w.int(m.id)
    }
}
