package zel.protocol.generated

data object QuitIn

object QuitInCodec : IncomingCodec<QuitIn> {
    override val name = "QuitIn"

    override fun read(r: PacketReader): QuitIn = QuitIn

    override fun write(w: PacketWriter, m: QuitIn) = writeQuitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeQuitIn(w: PacketWriter, m: QuitIn) {
    }
}
