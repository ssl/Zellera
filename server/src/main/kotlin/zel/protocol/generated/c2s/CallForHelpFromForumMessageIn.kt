package zel.protocol.generated

data class CallForHelpFromForumMessageIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val string1: String,
    val string2: String,
    val string3: String,
)

object CallForHelpFromForumMessageInCodec : IncomingCodec<CallForHelpFromForumMessageIn> {
    override val name = "CallForHelpFromForumMessageIn"

    override fun read(r: PacketReader): CallForHelpFromForumMessageIn = readCallForHelpFromForumMessageIn(r)

    override fun write(w: PacketWriter, m: CallForHelpFromForumMessageIn) = writeCallForHelpFromForumMessageIn(w, m)

    private fun readCallForHelpFromForumMessageIn(r: PacketReader): CallForHelpFromForumMessageIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return CallForHelpFromForumMessageIn(vInt1, vInt2, vInt3, vInt4, vString1, vString2, vString3)
    }

    private fun writeCallForHelpFromForumMessageIn(w: PacketWriter, m: CallForHelpFromForumMessageIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
