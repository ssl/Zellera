package zel.protocol.generated

data class UpdateGuildBadgeIn(
    val guildId: Int,
    val parts: List<Int>,
)

object UpdateGuildBadgeInCodec : IncomingCodec<UpdateGuildBadgeIn> {
    override val name = "UpdateGuildBadgeIn"

    override fun read(r: PacketReader): UpdateGuildBadgeIn = readUpdateGuildBadgeIn(r)

    override fun write(w: PacketWriter, m: UpdateGuildBadgeIn) = writeUpdateGuildBadgeIn(w, m)

    private fun readUpdateGuildBadgeIn(r: PacketReader): UpdateGuildBadgeIn {
        val vGuildId = r.int()
        val vParts = List(r.countInt(4)) { r.int() }
        return UpdateGuildBadgeIn(vGuildId, vParts)
    }

    private fun writeUpdateGuildBadgeIn(w: PacketWriter, m: UpdateGuildBadgeIn) {
        w.int(m.guildId)
        w.countInt(m.parts.size)
        m.parts.forEach { w.int(it) }
    }
}
