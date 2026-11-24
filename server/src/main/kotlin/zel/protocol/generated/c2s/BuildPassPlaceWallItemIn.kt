package zel.protocol.generated

data class BuildPassPlaceWallItemIn(
    val pageId: Int,
    val offerId: Int,
    val extraData: String,
    val wallPosition: String,
    val confirmed: Boolean?,
)

object BuildPassPlaceWallItemInCodec : IncomingCodec<BuildPassPlaceWallItemIn> {
    override val name = "BuildPassPlaceWallItemIn"

    override fun read(r: PacketReader): BuildPassPlaceWallItemIn = readBuildPassPlaceWallItemIn(r)

    override fun write(w: PacketWriter, m: BuildPassPlaceWallItemIn) = writeBuildPassPlaceWallItemIn(w, m)

    private fun readBuildPassPlaceWallItemIn(r: PacketReader): BuildPassPlaceWallItemIn {
        val vPageId = r.int()
        val vOfferId = r.int()
        val vExtraData = r.string()
        val vWallPosition = r.string()
        val vConfirmed = if (r.remaining > 0) r.boolean() else null
        return BuildPassPlaceWallItemIn(vPageId, vOfferId, vExtraData, vWallPosition, vConfirmed)
    }

    private fun writeBuildPassPlaceWallItemIn(w: PacketWriter, m: BuildPassPlaceWallItemIn) {
        w.int(m.pageId)
        w.int(m.offerId)
        w.string(m.extraData)
        w.string(m.wallPosition)
        m.confirmed?.let { w.boolean(it) }
    }
}
