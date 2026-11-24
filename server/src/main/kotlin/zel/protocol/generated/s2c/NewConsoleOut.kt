package zel.protocol.generated

data class NewConsoleOut(
    val int1: Int,
    val int2: Int,
    val rest: ByteArray,
)

object NewConsoleOutCodec : OutgoingCodec<NewConsoleOut> {
    override val name = "NewConsoleOut"

    override fun read(r: PacketReader): NewConsoleOut = readNewConsoleOut(r)

    override fun write(w: PacketWriter, m: NewConsoleOut) = writeNewConsoleOut(w, m)

    private fun readNewConsoleOut(r: PacketReader): NewConsoleOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vRest = r.rest()
        return NewConsoleOut(vInt1, vInt2, vRest)
    }

    private fun writeNewConsoleOut(w: PacketWriter, m: NewConsoleOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.bytes(m.rest)
    }
}
