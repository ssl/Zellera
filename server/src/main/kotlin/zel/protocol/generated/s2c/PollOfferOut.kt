package zel.protocol.generated

data class PollOfferOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val string3: String,
)

object PollOfferOutCodec : OutgoingCodec<PollOfferOut> {
    override val name = "PollOfferOut"

    override fun read(r: PacketReader): PollOfferOut = readPollOfferOut(r)

    override fun write(w: PacketWriter, m: PollOfferOut) = writePollOfferOut(w, m)

    private fun readPollOfferOut(r: PacketReader): PollOfferOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return PollOfferOut(vInt1, vString1, vString2, vString3)
    }

    private fun writePollOfferOut(w: PacketWriter, m: PollOfferOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
