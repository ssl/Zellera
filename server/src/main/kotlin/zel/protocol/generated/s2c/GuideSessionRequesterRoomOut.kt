package zel.protocol.generated

data class GuideSessionRequesterRoomOut(
    val int1: Int,
)

object GuideSessionRequesterRoomOutCodec : OutgoingCodec<GuideSessionRequesterRoomOut> {
    override val name = "GuideSessionRequesterRoomOut"

    override fun read(r: PacketReader): GuideSessionRequesterRoomOut = readGuideSessionRequesterRoomOut(r)

    override fun write(w: PacketWriter, m: GuideSessionRequesterRoomOut) = writeGuideSessionRequesterRoomOut(w, m)

    private fun readGuideSessionRequesterRoomOut(r: PacketReader): GuideSessionRequesterRoomOut {
        val vInt1 = r.int()
        return GuideSessionRequesterRoomOut(vInt1)
    }

    private fun writeGuideSessionRequesterRoomOut(w: PacketWriter, m: GuideSessionRequesterRoomOut) {
        w.int(m.int1)
    }
}
