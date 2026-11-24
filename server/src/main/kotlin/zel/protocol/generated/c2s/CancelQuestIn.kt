package zel.protocol.generated

data object CancelQuestIn

object CancelQuestInCodec : IncomingCodec<CancelQuestIn> {
    override val name = "CancelQuestIn"

    override fun read(r: PacketReader): CancelQuestIn = CancelQuestIn

    override fun write(w: PacketWriter, m: CancelQuestIn) = writeCancelQuestIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCancelQuestIn(w: PacketWriter, m: CancelQuestIn) {
    }
}
