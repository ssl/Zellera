package zel.protocol.generated

data object GuideSessionGetRequesterRoomIn

object GuideSessionGetRequesterRoomInCodec : IncomingCodec<GuideSessionGetRequesterRoomIn> {
    override val name = "GuideSessionGetRequesterRoomIn"

    override fun read(r: PacketReader): GuideSessionGetRequesterRoomIn = GuideSessionGetRequesterRoomIn

    override fun write(w: PacketWriter, m: GuideSessionGetRequesterRoomIn) = writeGuideSessionGetRequesterRoomIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideSessionGetRequesterRoomIn(w: PacketWriter, m: GuideSessionGetRequesterRoomIn) {
    }
}
