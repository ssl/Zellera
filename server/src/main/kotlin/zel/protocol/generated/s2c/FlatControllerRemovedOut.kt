package zel.protocol.generated

data class FlatControllerRemovedOut(
    val int1: Int,
    val int2: Int,
)

object FlatControllerRemovedOutCodec : OutgoingCodec<FlatControllerRemovedOut> {
    override val name = "FlatControllerRemovedOut"

    override fun read(r: PacketReader): FlatControllerRemovedOut = readFlatControllerRemovedOut(r)

    override fun write(w: PacketWriter, m: FlatControllerRemovedOut) = writeFlatControllerRemovedOut(w, m)

    private fun readFlatControllerRemovedOut(r: PacketReader): FlatControllerRemovedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return FlatControllerRemovedOut(vInt1, vInt2)
    }

    private fun writeFlatControllerRemovedOut(w: PacketWriter, m: FlatControllerRemovedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
