package zel.protocol.generated

data class SetRandomStateIn(
    val itemId: Int,
    val int1: Int,
)

object SetRandomStateInCodec : IncomingCodec<SetRandomStateIn> {
    override val name = "SetRandomStateIn"

    override fun read(r: PacketReader): SetRandomStateIn = readSetRandomStateIn(r)

    override fun write(w: PacketWriter, m: SetRandomStateIn) = writeSetRandomStateIn(w, m)

    private fun readSetRandomStateIn(r: PacketReader): SetRandomStateIn {
        val vItemId = r.int()
        val vInt1 = r.int()
        return SetRandomStateIn(vItemId, vInt1)
    }

    private fun writeSetRandomStateIn(w: PacketWriter, m: SetRandomStateIn) {
        w.int(m.itemId)
        w.int(m.int1)
    }
}
