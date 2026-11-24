package zel.protocol.generated

data class FlatAccessibleOut(
    val int1: Int,
    val string1: String,
)

object FlatAccessibleOutCodec : OutgoingCodec<FlatAccessibleOut> {
    override val name = "FlatAccessibleOut"

    override fun read(r: PacketReader): FlatAccessibleOut = readFlatAccessibleOut(r)

    override fun write(w: PacketWriter, m: FlatAccessibleOut) = writeFlatAccessibleOut(w, m)

    private fun readFlatAccessibleOut(r: PacketReader): FlatAccessibleOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return FlatAccessibleOut(vInt1, vString1)
    }

    private fun writeFlatAccessibleOut(w: PacketWriter, m: FlatAccessibleOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
