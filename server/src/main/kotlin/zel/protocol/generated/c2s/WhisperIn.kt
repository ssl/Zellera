package zel.protocol.generated

data class WhisperIn(
    val message: String,
    val styleId: Int,
)

object WhisperInCodec : IncomingCodec<WhisperIn> {
    override val name = "WhisperIn"

    override fun read(r: PacketReader): WhisperIn = readWhisperIn(r)

    override fun write(w: PacketWriter, m: WhisperIn) = writeWhisperIn(w, m)

    private fun readWhisperIn(r: PacketReader): WhisperIn {
        val vMessage = r.string()
        val vStyleId = r.int()
        return WhisperIn(vMessage, vStyleId)
    }

    private fun writeWhisperIn(w: PacketWriter, m: WhisperIn) {
        w.string(m.message)
        w.int(m.styleId)
    }
}
