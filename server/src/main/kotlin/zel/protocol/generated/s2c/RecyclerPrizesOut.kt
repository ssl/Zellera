package zel.protocol.generated

data class RecyclerPrizesOut(
    val rest: ByteArray,
)

object RecyclerPrizesOutCodec : OutgoingCodec<RecyclerPrizesOut> {
    override val name = "RecyclerPrizesOut"

    override fun read(r: PacketReader): RecyclerPrizesOut = readRecyclerPrizesOut(r)

    override fun write(w: PacketWriter, m: RecyclerPrizesOut) = writeRecyclerPrizesOut(w, m)

    private fun readRecyclerPrizesOut(r: PacketReader): RecyclerPrizesOut {
        val vRest = r.rest()
        return RecyclerPrizesOut(vRest)
    }

    private fun writeRecyclerPrizesOut(w: PacketWriter, m: RecyclerPrizesOut) {
        w.bytes(m.rest)
    }
}
