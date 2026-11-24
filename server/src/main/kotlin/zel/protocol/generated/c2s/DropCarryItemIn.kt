package zel.protocol.generated

data object DropCarryItemIn

object DropCarryItemInCodec : IncomingCodec<DropCarryItemIn> {
    override val name = "DropCarryItemIn"

    override fun read(r: PacketReader): DropCarryItemIn = DropCarryItemIn

    override fun write(w: PacketWriter, m: DropCarryItemIn) = writeDropCarryItemIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeDropCarryItemIn(w: PacketWriter, m: DropCarryItemIn) {
    }
}
