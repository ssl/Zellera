package zel.protocol.generated

data class GetForumsListIn(
    val mode: Int,
    val offset: Int,
    val int1: Int,
)

object GetForumsListInCodec : IncomingCodec<GetForumsListIn> {
    override val name = "GetForumsListIn"

    override fun read(r: PacketReader): GetForumsListIn = readGetForumsListIn(r)

    override fun write(w: PacketWriter, m: GetForumsListIn) = writeGetForumsListIn(w, m)

    private fun readGetForumsListIn(r: PacketReader): GetForumsListIn {
        val vMode = r.int()
        val vOffset = r.int()
        val vInt1 = r.int()
        return GetForumsListIn(vMode, vOffset, vInt1)
    }

    private fun writeGetForumsListIn(w: PacketWriter, m: GetForumsListIn) {
        w.int(m.mode)
        w.int(m.offset)
        w.int(m.int1)
    }
}
