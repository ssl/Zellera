package zel.protocol.generated

data class ClickCharacterIn(
    val int1: Int,
)

object ClickCharacterInCodec : IncomingCodec<ClickCharacterIn> {
    override val name = "ClickCharacterIn"

    override fun read(r: PacketReader): ClickCharacterIn = readClickCharacterIn(r)

    override fun write(w: PacketWriter, m: ClickCharacterIn) = writeClickCharacterIn(w, m)

    private fun readClickCharacterIn(r: PacketReader): ClickCharacterIn {
        val vInt1 = r.int()
        return ClickCharacterIn(vInt1)
    }

    private fun writeClickCharacterIn(w: PacketWriter, m: ClickCharacterIn) {
        w.int(m.int1)
    }
}
