package zel.protocol.generated

data class CurrentTimingCodeOut(
    val schedulingStr: String,
    val code: String,
)

object CurrentTimingCodeOutCodec : OutgoingCodec<CurrentTimingCodeOut> {
    override val name = "CurrentTimingCodeOut"

    override fun read(r: PacketReader): CurrentTimingCodeOut = readCurrentTimingCodeOut(r)

    override fun write(w: PacketWriter, m: CurrentTimingCodeOut) = writeCurrentTimingCodeOut(w, m)

    private fun readCurrentTimingCodeOut(r: PacketReader): CurrentTimingCodeOut {
        val vSchedulingStr = r.string()
        val vCode = r.string()
        return CurrentTimingCodeOut(vSchedulingStr, vCode)
    }

    private fun writeCurrentTimingCodeOut(w: PacketWriter, m: CurrentTimingCodeOut) {
        w.string(m.schedulingStr)
        w.string(m.code)
    }
}
