package zel.protocol.generated

data class PlayerChatlogOut(
    val int1: Int,
    val string1: String,
    val rest: ByteArray,
)

object PlayerChatlogOutCodec : OutgoingCodec<PlayerChatlogOut> {
    override val name = "PlayerChatlogOut"

    override fun read(r: PacketReader): PlayerChatlogOut = readPlayerChatlogOut(r)

    override fun write(w: PacketWriter, m: PlayerChatlogOut) = writePlayerChatlogOut(w, m)

    private fun readPlayerChatlogOut(r: PacketReader): PlayerChatlogOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vRest = r.rest()
        return PlayerChatlogOut(vInt1, vString1, vRest)
    }

    private fun writePlayerChatlogOut(w: PacketWriter, m: PlayerChatlogOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.bytes(m.rest)
    }
}
