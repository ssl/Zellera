package zel.protocol.generated

data object GetBadgePointLimitsIn

object GetBadgePointLimitsInCodec : IncomingCodec<GetBadgePointLimitsIn> {
    override val name = "GetBadgePointLimitsIn"

    override fun read(r: PacketReader): GetBadgePointLimitsIn = GetBadgePointLimitsIn

    override fun write(w: PacketWriter, m: GetBadgePointLimitsIn) = writeGetBadgePointLimitsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBadgePointLimitsIn(w: PacketWriter, m: GetBadgePointLimitsIn) {
    }
}
