package zel.protocol.generated

data class SetTargetedOfferStateIn(
    val id: Int,
    val state: Int,
)

object SetTargetedOfferStateInCodec : IncomingCodec<SetTargetedOfferStateIn> {
    override val name = "SetTargetedOfferStateIn"

    override fun read(r: PacketReader): SetTargetedOfferStateIn = readSetTargetedOfferStateIn(r)

    override fun write(w: PacketWriter, m: SetTargetedOfferStateIn) = writeSetTargetedOfferStateIn(w, m)

    private fun readSetTargetedOfferStateIn(r: PacketReader): SetTargetedOfferStateIn {
        val vId = r.int()
        val vState = r.int()
        return SetTargetedOfferStateIn(vId, vState)
    }

    private fun writeSetTargetedOfferStateIn(w: PacketWriter, m: SetTargetedOfferStateIn) {
        w.int(m.id)
        w.int(m.state)
    }
}
