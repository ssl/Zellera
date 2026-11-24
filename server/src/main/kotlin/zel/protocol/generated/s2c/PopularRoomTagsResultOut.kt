package zel.protocol.generated

data class PopularRoomTagsResultOut(
    val tags: List<TagsItem>,
) {
    data class TagsItem(
        val tag: String,
        val userCount: Int,
    )
}

object PopularRoomTagsResultOutCodec : OutgoingCodec<PopularRoomTagsResultOut> {
    override val name = "PopularRoomTagsResultOut"

    override fun read(r: PacketReader): PopularRoomTagsResultOut = readPopularRoomTagsResultOut(r)

    override fun write(w: PacketWriter, m: PopularRoomTagsResultOut) = writePopularRoomTagsResultOut(w, m)

    private fun readPopularRoomTagsResultOutTagsItem(r: PacketReader): PopularRoomTagsResultOut.TagsItem {
        val vTag = r.string()
        val vUserCount = r.int()
        return PopularRoomTagsResultOut.TagsItem(vTag, vUserCount)
    }

    private fun readPopularRoomTagsResultOut(r: PacketReader): PopularRoomTagsResultOut {
        val vTags = List(r.countInt(6)) { readPopularRoomTagsResultOutTagsItem(r) }
        return PopularRoomTagsResultOut(vTags)
    }

    private fun writePopularRoomTagsResultOutTagsItem(w: PacketWriter, m: PopularRoomTagsResultOut.TagsItem) {
        w.string(m.tag)
        w.int(m.userCount)
    }

    private fun writePopularRoomTagsResultOut(w: PacketWriter, m: PopularRoomTagsResultOut) {
        w.countInt(m.tags.size)
        m.tags.forEach { writePopularRoomTagsResultOutTagsItem(w, it) }
    }
}
