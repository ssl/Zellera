package zel.protocol.generated

data object NewNavigatorInitIn

object NewNavigatorInitInCodec : IncomingCodec<NewNavigatorInitIn> {
    override val name = "NewNavigatorInitIn"

    override fun read(r: PacketReader): NewNavigatorInitIn = NewNavigatorInitIn

    override fun write(w: PacketWriter, m: NewNavigatorInitIn) = writeNewNavigatorInitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeNewNavigatorInitIn(w: PacketWriter, m: NewNavigatorInitIn) {
    }
}
