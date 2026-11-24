package zel.protocol.generated

data object GuideSessionInviteRequesterIn

object GuideSessionInviteRequesterInCodec : IncomingCodec<GuideSessionInviteRequesterIn> {
    override val name = "GuideSessionInviteRequesterIn"

    override fun read(r: PacketReader): GuideSessionInviteRequesterIn = GuideSessionInviteRequesterIn

    override fun write(w: PacketWriter, m: GuideSessionInviteRequesterIn) = writeGuideSessionInviteRequesterIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideSessionInviteRequesterIn(w: PacketWriter, m: GuideSessionInviteRequesterIn) {
    }
}
