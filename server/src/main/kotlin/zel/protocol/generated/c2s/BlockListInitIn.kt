package zel.protocol.generated

data object BlockListInitIn

object BlockListInitInCodec : IncomingCodec<BlockListInitIn> {
    override val name = "BlockListInitIn"

    override fun read(r: PacketReader): BlockListInitIn = BlockListInitIn

    override fun write(w: PacketWriter, m: BlockListInitIn) = writeBlockListInitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeBlockListInitIn(w: PacketWriter, m: BlockListInitIn) {
    }
}
