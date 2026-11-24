package zel.protocol.generated

data class IgnorePlayerIn(
    val userId: Int,
)

object IgnorePlayerInCodec : IncomingCodec<IgnorePlayerIn> {
    override val name = "IgnorePlayerIn"

    override fun read(r: PacketReader): IgnorePlayerIn = readIgnorePlayerIn(r)

    override fun write(w: PacketWriter, m: IgnorePlayerIn) = writeIgnorePlayerIn(w, m)

    private fun readIgnorePlayerIn(r: PacketReader): IgnorePlayerIn {
        val vUserId = r.int()
        return IgnorePlayerIn(vUserId)
    }

    private fun writeIgnorePlayerIn(w: PacketWriter, m: IgnorePlayerIn) {
        w.int(m.userId)
    }
}
