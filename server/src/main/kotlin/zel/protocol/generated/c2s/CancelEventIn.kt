package zel.protocol.generated

data class CancelEventIn(
    val int1: Int,
)

object CancelEventInCodec : IncomingCodec<CancelEventIn> {
    override val name = "CancelEventIn"

    override fun read(r: PacketReader): CancelEventIn = readCancelEventIn(r)

    override fun write(w: PacketWriter, m: CancelEventIn) = writeCancelEventIn(w, m)

    private fun readCancelEventIn(r: PacketReader): CancelEventIn {
        val vInt1 = r.int()
        return CancelEventIn(vInt1)
    }

    private fun writeCancelEventIn(w: PacketWriter, m: CancelEventIn) {
        w.int(m.int1)
    }
}
