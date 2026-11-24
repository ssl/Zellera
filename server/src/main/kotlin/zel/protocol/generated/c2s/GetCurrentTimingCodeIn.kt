package zel.protocol.generated

data class GetCurrentTimingCodeIn(
    val schedulingStr: String,
)

object GetCurrentTimingCodeInCodec : IncomingCodec<GetCurrentTimingCodeIn> {
    override val name = "GetCurrentTimingCodeIn"

    override fun read(r: PacketReader): GetCurrentTimingCodeIn = readGetCurrentTimingCodeIn(r)

    override fun write(w: PacketWriter, m: GetCurrentTimingCodeIn) = writeGetCurrentTimingCodeIn(w, m)

    private fun readGetCurrentTimingCodeIn(r: PacketReader): GetCurrentTimingCodeIn {
        val vSchedulingStr = r.string()
        return GetCurrentTimingCodeIn(vSchedulingStr)
    }

    private fun writeGetCurrentTimingCodeIn(w: PacketWriter, m: GetCurrentTimingCodeIn) {
        w.string(m.schedulingStr)
    }
}
