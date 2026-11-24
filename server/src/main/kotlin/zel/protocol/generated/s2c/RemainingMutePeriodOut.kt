package zel.protocol.generated

data class RemainingMutePeriodOut(
    val int1: Int,
)

object RemainingMutePeriodOutCodec : OutgoingCodec<RemainingMutePeriodOut> {
    override val name = "RemainingMutePeriodOut"

    override fun read(r: PacketReader): RemainingMutePeriodOut = readRemainingMutePeriodOut(r)

    override fun write(w: PacketWriter, m: RemainingMutePeriodOut) = writeRemainingMutePeriodOut(w, m)

    private fun readRemainingMutePeriodOut(r: PacketReader): RemainingMutePeriodOut {
        val vInt1 = r.int()
        return RemainingMutePeriodOut(vInt1)
    }

    private fun writeRemainingMutePeriodOut(w: PacketWriter, m: RemainingMutePeriodOut) {
        w.int(m.int1)
    }
}
