package zel.protocol.generated

data object LimitedEditionSoldOutOut

object LimitedEditionSoldOutOutCodec : OutgoingCodec<LimitedEditionSoldOutOut> {
    override val name = "LimitedEditionSoldOutOut"

    override fun read(r: PacketReader): LimitedEditionSoldOutOut = LimitedEditionSoldOutOut

    override fun write(w: PacketWriter, m: LimitedEditionSoldOutOut) = writeLimitedEditionSoldOutOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeLimitedEditionSoldOutOut(w: PacketWriter, m: LimitedEditionSoldOutOut) {
    }
}
