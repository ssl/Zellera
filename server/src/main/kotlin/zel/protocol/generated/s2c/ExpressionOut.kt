package zel.protocol.generated

data class ExpressionOut(
    val int1: Int,
    val int2: Int,
)

object ExpressionOutCodec : OutgoingCodec<ExpressionOut> {
    override val name = "ExpressionOut"

    override fun read(r: PacketReader): ExpressionOut = readExpressionOut(r)

    override fun write(w: PacketWriter, m: ExpressionOut) = writeExpressionOut(w, m)

    private fun readExpressionOut(r: PacketReader): ExpressionOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ExpressionOut(vInt1, vInt2)
    }

    private fun writeExpressionOut(w: PacketWriter, m: ExpressionOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
