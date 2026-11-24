package zel.protocol.generated

data class AvailabilityStatusOut(
    val boolean1: Boolean,
    val boolean2: Boolean,
    val trailing1: Boolean?,
)

object AvailabilityStatusOutCodec : OutgoingCodec<AvailabilityStatusOut> {
    override val name = "AvailabilityStatusOut"

    override fun read(r: PacketReader): AvailabilityStatusOut = readAvailabilityStatusOut(r)

    override fun write(w: PacketWriter, m: AvailabilityStatusOut) = writeAvailabilityStatusOut(w, m)

    private fun readAvailabilityStatusOut(r: PacketReader): AvailabilityStatusOut {
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.boolean() else null
        return AvailabilityStatusOut(vBoolean1, vBoolean2, vTrailing1)
    }

    private fun writeAvailabilityStatusOut(w: PacketWriter, m: AvailabilityStatusOut) {
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        m.trailing1?.let { w.boolean(it) }
    }
}
