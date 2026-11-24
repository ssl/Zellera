package zel.protocol.generated

data class MovePetIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object MovePetInCodec : IncomingCodec<MovePetIn> {
    override val name = "MovePetIn"

    override fun read(r: PacketReader): MovePetIn = readMovePetIn(r)

    override fun write(w: PacketWriter, m: MovePetIn) = writeMovePetIn(w, m)

    private fun readMovePetIn(r: PacketReader): MovePetIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return MovePetIn(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeMovePetIn(w: PacketWriter, m: MovePetIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
