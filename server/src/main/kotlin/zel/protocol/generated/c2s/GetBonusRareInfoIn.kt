package zel.protocol.generated

data object GetBonusRareInfoIn

object GetBonusRareInfoInCodec : IncomingCodec<GetBonusRareInfoIn> {
    override val name = "GetBonusRareInfoIn"

    override fun read(r: PacketReader): GetBonusRareInfoIn = GetBonusRareInfoIn

    override fun write(w: PacketWriter, m: GetBonusRareInfoIn) = writeGetBonusRareInfoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBonusRareInfoIn(w: PacketWriter, m: GetBonusRareInfoIn) {
    }
}
