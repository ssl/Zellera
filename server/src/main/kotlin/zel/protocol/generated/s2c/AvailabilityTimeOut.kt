package zel.protocol.generated

data class AvailabilityTimeOut(
    val int1: Int,
    val int2: Int,
)

object AvailabilityTimeOutCodec : OutgoingCodec<AvailabilityTimeOut> {
    override val name = "AvailabilityTimeOut"

    override fun read(r: PacketReader): AvailabilityTimeOut = readAvailabilityTimeOut(r)

    override fun write(w: PacketWriter, m: AvailabilityTimeOut) = writeAvailabilityTimeOut(w, m)

    private fun readAvailabilityTimeOut(r: PacketReader): AvailabilityTimeOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return AvailabilityTimeOut(vInt1, vInt2)
    }

    private fun writeAvailabilityTimeOut(w: PacketWriter, m: AvailabilityTimeOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
