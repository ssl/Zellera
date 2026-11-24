package zel.protocol.generated

data class NotEnoughBalanceOut(
    val boolean1: Boolean,
    val boolean2: Boolean,
    val trailing1: Int?,
)

object NotEnoughBalanceOutCodec : OutgoingCodec<NotEnoughBalanceOut> {
    override val name = "NotEnoughBalanceOut"

    override fun read(r: PacketReader): NotEnoughBalanceOut = readNotEnoughBalanceOut(r)

    override fun write(w: PacketWriter, m: NotEnoughBalanceOut) = writeNotEnoughBalanceOut(w, m)

    private fun readNotEnoughBalanceOut(r: PacketReader): NotEnoughBalanceOut {
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return NotEnoughBalanceOut(vBoolean1, vBoolean2, vTrailing1)
    }

    private fun writeNotEnoughBalanceOut(w: PacketWriter, m: NotEnoughBalanceOut) {
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        m.trailing1?.let { w.int(it) }
    }
}
