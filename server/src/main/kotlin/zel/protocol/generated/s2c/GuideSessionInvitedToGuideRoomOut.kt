package zel.protocol.generated

data class GuideSessionInvitedToGuideRoomOut(
    val int1: Int,
    val string1: String,
)

object GuideSessionInvitedToGuideRoomOutCodec : OutgoingCodec<GuideSessionInvitedToGuideRoomOut> {
    override val name = "GuideSessionInvitedToGuideRoomOut"

    override fun read(r: PacketReader): GuideSessionInvitedToGuideRoomOut = readGuideSessionInvitedToGuideRoomOut(r)

    override fun write(w: PacketWriter, m: GuideSessionInvitedToGuideRoomOut) = writeGuideSessionInvitedToGuideRoomOut(w, m)

    private fun readGuideSessionInvitedToGuideRoomOut(r: PacketReader): GuideSessionInvitedToGuideRoomOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return GuideSessionInvitedToGuideRoomOut(vInt1, vString1)
    }

    private fun writeGuideSessionInvitedToGuideRoomOut(w: PacketWriter, m: GuideSessionInvitedToGuideRoomOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
