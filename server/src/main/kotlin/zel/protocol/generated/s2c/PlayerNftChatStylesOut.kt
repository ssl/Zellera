package zel.protocol.generated

data class PlayerNftChatStylesOut(
    val list1: List<Int>,
)

object PlayerNftChatStylesOutCodec : OutgoingCodec<PlayerNftChatStylesOut> {
    override val name = "PlayerNftChatStylesOut"

    override fun read(r: PacketReader): PlayerNftChatStylesOut = readPlayerNftChatStylesOut(r)

    override fun write(w: PacketWriter, m: PlayerNftChatStylesOut) = writePlayerNftChatStylesOut(w, m)

    private fun readPlayerNftChatStylesOut(r: PacketReader): PlayerNftChatStylesOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return PlayerNftChatStylesOut(vList1)
    }

    private fun writePlayerNftChatStylesOut(w: PacketWriter, m: PlayerNftChatStylesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
