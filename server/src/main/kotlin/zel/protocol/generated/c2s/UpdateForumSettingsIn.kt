package zel.protocol.generated

data class UpdateForumSettingsIn(
    val guildId: Int,
    val canRead: Int,
    val postMessages: Int,
    val postThreads: Int,
    val modForum: Int,
)

object UpdateForumSettingsInCodec : IncomingCodec<UpdateForumSettingsIn> {
    override val name = "UpdateForumSettingsIn"

    override fun read(r: PacketReader): UpdateForumSettingsIn = readUpdateForumSettingsIn(r)

    override fun write(w: PacketWriter, m: UpdateForumSettingsIn) = writeUpdateForumSettingsIn(w, m)

    private fun readUpdateForumSettingsIn(r: PacketReader): UpdateForumSettingsIn {
        val vGuildId = r.int()
        val vCanRead = r.int()
        val vPostMessages = r.int()
        val vPostThreads = r.int()
        val vModForum = r.int()
        return UpdateForumSettingsIn(vGuildId, vCanRead, vPostMessages, vPostThreads, vModForum)
    }

    private fun writeUpdateForumSettingsIn(w: PacketWriter, m: UpdateForumSettingsIn) {
        w.int(m.guildId)
        w.int(m.canRead)
        w.int(m.postMessages)
        w.int(m.postThreads)
        w.int(m.modForum)
    }
}
