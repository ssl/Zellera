package zel.protocol.generated

data class UnignorePlayerIn(
    val int1: Int,
)

object UnignorePlayerInCodec : IncomingCodec<UnignorePlayerIn> {
    override val name = "UnignorePlayerIn"

    override fun read(r: PacketReader): UnignorePlayerIn = readUnignorePlayerIn(r)

    override fun write(w: PacketWriter, m: UnignorePlayerIn) = writeUnignorePlayerIn(w, m)

    private fun readUnignorePlayerIn(r: PacketReader): UnignorePlayerIn {
        val vInt1 = r.int()
        return UnignorePlayerIn(vInt1)
    }

    private fun writeUnignorePlayerIn(w: PacketWriter, m: UnignorePlayerIn) {
        w.int(m.int1)
    }
}
