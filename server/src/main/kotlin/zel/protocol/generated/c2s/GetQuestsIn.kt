package zel.protocol.generated

data object GetQuestsIn

object GetQuestsInCodec : IncomingCodec<GetQuestsIn> {
    override val name = "GetQuestsIn"

    override fun read(r: PacketReader): GetQuestsIn = GetQuestsIn

    override fun write(w: PacketWriter, m: GetQuestsIn) = writeGetQuestsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetQuestsIn(w: PacketWriter, m: GetQuestsIn) {
    }
}
