package zel.protocol.generated

data class BuildPassPlaceRoomItemIn(
    val pageId: Int,
    val offerId: Int,
    val extraData: String,
    val x: Int,
    val y: Int,
    val rotation: Int,
    val confirmed: Boolean?,
)

object BuildPassPlaceRoomItemInCodec : IncomingCodec<BuildPassPlaceRoomItemIn> {
    override val name = "BuildPassPlaceRoomItemIn"

    override fun read(r: PacketReader): BuildPassPlaceRoomItemIn = readBuildPassPlaceRoomItemIn(r)

    override fun write(w: PacketWriter, m: BuildPassPlaceRoomItemIn) = writeBuildPassPlaceRoomItemIn(w, m)

    private fun readBuildPassPlaceRoomItemIn(r: PacketReader): BuildPassPlaceRoomItemIn {
        val vPageId = r.int()
        val vOfferId = r.int()
        val vExtraData = r.string()
        val vX = r.int()
        val vY = r.int()
        val vRotation = r.int()
        val vConfirmed = if (r.remaining > 0) r.boolean() else null
        return BuildPassPlaceRoomItemIn(vPageId, vOfferId, vExtraData, vX, vY, vRotation, vConfirmed)
    }

    private fun writeBuildPassPlaceRoomItemIn(w: PacketWriter, m: BuildPassPlaceRoomItemIn) {
        w.int(m.pageId)
        w.int(m.offerId)
        w.string(m.extraData)
        w.int(m.x)
        w.int(m.y)
        w.int(m.rotation)
        m.confirmed?.let { w.boolean(it) }
    }
}
