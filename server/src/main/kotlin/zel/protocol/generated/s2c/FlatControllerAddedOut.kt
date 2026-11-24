package zel.protocol.generated

data class FlatControllerAddedOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object FlatControllerAddedOutCodec : OutgoingCodec<FlatControllerAddedOut> {
    override val name = "FlatControllerAddedOut"

    override fun read(r: PacketReader): FlatControllerAddedOut = readFlatControllerAddedOut(r)

    override fun write(w: PacketWriter, m: FlatControllerAddedOut) = writeFlatControllerAddedOut(w, m)

    private fun readFlatControllerAddedOut(r: PacketReader): FlatControllerAddedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return FlatControllerAddedOut(vInt1, vInt2, vString1)
    }

    private fun writeFlatControllerAddedOut(w: PacketWriter, m: FlatControllerAddedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
