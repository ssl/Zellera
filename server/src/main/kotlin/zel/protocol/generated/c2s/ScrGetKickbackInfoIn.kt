package zel.protocol.generated

data object ScrGetKickbackInfoIn

object ScrGetKickbackInfoInCodec : IncomingCodec<ScrGetKickbackInfoIn> {
    override val name = "ScrGetKickbackInfoIn"

    override fun read(r: PacketReader): ScrGetKickbackInfoIn = ScrGetKickbackInfoIn

    override fun write(w: PacketWriter, m: ScrGetKickbackInfoIn) = writeScrGetKickbackInfoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeScrGetKickbackInfoIn(w: PacketWriter, m: ScrGetKickbackInfoIn) {
    }
}
