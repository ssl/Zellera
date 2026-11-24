package zel.protocol.generated

data class GetFaqTextIn(
    val int1: Int,
)

object GetFaqTextInCodec : IncomingCodec<GetFaqTextIn> {
    override val name = "GetFaqTextIn"

    override fun read(r: PacketReader): GetFaqTextIn = readGetFaqTextIn(r)

    override fun write(w: PacketWriter, m: GetFaqTextIn) = writeGetFaqTextIn(w, m)

    private fun readGetFaqTextIn(r: PacketReader): GetFaqTextIn {
        val vInt1 = r.int()
        return GetFaqTextIn(vInt1)
    }

    private fun writeGetFaqTextIn(w: PacketWriter, m: GetFaqTextIn) {
        w.int(m.int1)
    }
}
