package zel.protocol.generated

data class ShoutIn(
    val string1: String,
    val int1: Int,
)

object ShoutInCodec : IncomingCodec<ShoutIn> {
    override val name = "ShoutIn"

    override fun read(r: PacketReader): ShoutIn = readShoutIn(r)

    override fun write(w: PacketWriter, m: ShoutIn) = writeShoutIn(w, m)

    private fun readShoutIn(r: PacketReader): ShoutIn {
        val vString1 = r.string()
        val vInt1 = r.int()
        return ShoutIn(vString1, vInt1)
    }

    private fun writeShoutIn(w: PacketWriter, m: ShoutIn) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
