package zel.protocol.generated

data class ChatOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
    val int4: Int,
    val trailing1: Int?,
    val trailing2: Int?,
    val trailing3: Boolean?,
    val trailing4: Trailing4?,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
    )

    data class Trailing4(
        val int1: Int,
        val int2: Int,
    )
}

object ChatOutCodec : OutgoingCodec<ChatOut> {
    override val name = "ChatOut"

    override fun read(r: PacketReader): ChatOut = readChatOut(r)

    override fun write(w: PacketWriter, m: ChatOut) = writeChatOut(w, m)

    private fun readChatOutList1Item(r: PacketReader): ChatOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        return ChatOut.List1Item(vString1, vString2, vBoolean1)
    }

    private fun readChatOutTrailing4(r: PacketReader): ChatOut.Trailing4 {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ChatOut.Trailing4(vInt1, vInt2)
    }

    private fun readChatOut(r: PacketReader): ChatOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(5)) { readChatOutList1Item(r) }
        val vInt4 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        val vTrailing2 = if (r.remaining > 0) r.int() else null
        val vTrailing3 = if (r.remaining > 0) r.boolean() else null
        val vTrailing4 = if (r.remaining > 0) readChatOutTrailing4(r) else null
        return ChatOut(vInt1, vString1, vInt2, vInt3, vList1, vInt4, vTrailing1, vTrailing2, vTrailing3, vTrailing4)
    }

    private fun writeChatOutList1Item(w: PacketWriter, m: ChatOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
    }

    private fun writeChatOutTrailing4(w: PacketWriter, m: ChatOut.Trailing4) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeChatOut(w: PacketWriter, m: ChatOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeChatOutList1Item(w, it) }
        w.int(m.int4)
        m.trailing1?.let { w.int(it) }
        m.trailing2?.let { w.int(it) }
        m.trailing3?.let { w.boolean(it) }
        m.trailing4?.let { writeChatOutTrailing4(w, it) }
    }
}
