package zel.protocol.generated

data class EditEventIn(
    val id: Int,
    val promotionName: String,
    val promotionDescription: String,
)

object EditEventInCodec : IncomingCodec<EditEventIn> {
    override val name = "EditEventIn"

    override fun read(r: PacketReader): EditEventIn = readEditEventIn(r)

    override fun write(w: PacketWriter, m: EditEventIn) = writeEditEventIn(w, m)

    private fun readEditEventIn(r: PacketReader): EditEventIn {
        val vId = r.int()
        val vPromotionName = r.string()
        val vPromotionDescription = r.string()
        return EditEventIn(vId, vPromotionName, vPromotionDescription)
    }

    private fun writeEditEventIn(w: PacketWriter, m: EditEventIn) {
        w.int(m.id)
        w.string(m.promotionName)
        w.string(m.promotionDescription)
    }
}
