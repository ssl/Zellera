package zel.protocol.generated

data class GuideTicketResolutionOut(
    val int1: Int,
)

object GuideTicketResolutionOutCodec : OutgoingCodec<GuideTicketResolutionOut> {
    override val name = "GuideTicketResolutionOut"

    override fun read(r: PacketReader): GuideTicketResolutionOut = readGuideTicketResolutionOut(r)

    override fun write(w: PacketWriter, m: GuideTicketResolutionOut) = writeGuideTicketResolutionOut(w, m)

    private fun readGuideTicketResolutionOut(r: PacketReader): GuideTicketResolutionOut {
        val vInt1 = r.int()
        return GuideTicketResolutionOut(vInt1)
    }

    private fun writeGuideTicketResolutionOut(w: PacketWriter, m: GuideTicketResolutionOut) {
        w.int(m.int1)
    }
}
