package zel.protocol.generated

data object GetSeasonalQuestsOnlyIn

object GetSeasonalQuestsOnlyInCodec : IncomingCodec<GetSeasonalQuestsOnlyIn> {
    override val name = "GetSeasonalQuestsOnlyIn"

    override fun read(r: PacketReader): GetSeasonalQuestsOnlyIn = GetSeasonalQuestsOnlyIn

    override fun write(w: PacketWriter, m: GetSeasonalQuestsOnlyIn) = writeGetSeasonalQuestsOnlyIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSeasonalQuestsOnlyIn(w: PacketWriter, m: GetSeasonalQuestsOnlyIn) {
    }
}
