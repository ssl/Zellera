package zel.protocol.generated

data class SelfDonationResultOut(
    val int1: Int,
)

object SelfDonationResultOutCodec : OutgoingCodec<SelfDonationResultOut> {
    override val name = "SelfDonationResultOut"

    override fun read(r: PacketReader): SelfDonationResultOut = readSelfDonationResultOut(r)

    override fun write(w: PacketWriter, m: SelfDonationResultOut) = writeSelfDonationResultOut(w, m)

    private fun readSelfDonationResultOut(r: PacketReader): SelfDonationResultOut {
        val vInt1 = r.int()
        return SelfDonationResultOut(vInt1)
    }

    private fun writeSelfDonationResultOut(w: PacketWriter, m: SelfDonationResultOut) {
        w.int(m.int1)
    }
}
