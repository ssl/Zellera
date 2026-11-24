package zel.protocol.generated

data class GuideTicketCreationResultOut(
    val int1: Int,
)

object GuideTicketCreationResultOutCodec : OutgoingCodec<GuideTicketCreationResultOut> {
    override val name = "GuideTicketCreationResultOut"

    override fun read(r: PacketReader): GuideTicketCreationResultOut = readGuideTicketCreationResultOut(r)

    override fun write(w: PacketWriter, m: GuideTicketCreationResultOut) = writeGuideTicketCreationResultOut(w, m)

    private fun readGuideTicketCreationResultOut(r: PacketReader): GuideTicketCreationResultOut {
        val vInt1 = r.int()
        return GuideTicketCreationResultOut(vInt1)
    }

    private fun writeGuideTicketCreationResultOut(w: PacketWriter, m: GuideTicketCreationResultOut) {
        w.int(m.int1)
    }
}
