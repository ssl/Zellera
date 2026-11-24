package zel.protocol.generated

data class CallForHelpFromPhotoIn(
    val photoId: String,
    val roomId: Int,
    val creatorId: Int,
    val topicId: Int,
    val itemId: Int,
    val reporterName: String,
    val reporterEmail: String,
)

object CallForHelpFromPhotoInCodec : IncomingCodec<CallForHelpFromPhotoIn> {
    override val name = "CallForHelpFromPhotoIn"

    override fun read(r: PacketReader): CallForHelpFromPhotoIn = readCallForHelpFromPhotoIn(r)

    override fun write(w: PacketWriter, m: CallForHelpFromPhotoIn) = writeCallForHelpFromPhotoIn(w, m)

    private fun readCallForHelpFromPhotoIn(r: PacketReader): CallForHelpFromPhotoIn {
        val vPhotoId = r.string()
        val vRoomId = r.int()
        val vCreatorId = r.int()
        val vTopicId = r.int()
        val vItemId = r.int()
        val vReporterName = r.string()
        val vReporterEmail = r.string()
        return CallForHelpFromPhotoIn(vPhotoId, vRoomId, vCreatorId, vTopicId, vItemId, vReporterName, vReporterEmail)
    }

    private fun writeCallForHelpFromPhotoIn(w: PacketWriter, m: CallForHelpFromPhotoIn) {
        w.string(m.photoId)
        w.int(m.roomId)
        w.int(m.creatorId)
        w.int(m.topicId)
        w.int(m.itemId)
        w.string(m.reporterName)
        w.string(m.reporterEmail)
    }
}
