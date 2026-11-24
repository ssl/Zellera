package zel.protocol.generated

data class DeactivateGuildIn(
    val guildId: Int,
)

object DeactivateGuildInCodec : IncomingCodec<DeactivateGuildIn> {
    override val name = "DeactivateGuildIn"

    override fun read(r: PacketReader): DeactivateGuildIn = readDeactivateGuildIn(r)

    override fun write(w: PacketWriter, m: DeactivateGuildIn) = writeDeactivateGuildIn(w, m)

    private fun readDeactivateGuildIn(r: PacketReader): DeactivateGuildIn {
        val vGuildId = r.int()
        return DeactivateGuildIn(vGuildId)
    }

    private fun writeDeactivateGuildIn(w: PacketWriter, m: DeactivateGuildIn) {
        w.int(m.guildId)
    }
}
