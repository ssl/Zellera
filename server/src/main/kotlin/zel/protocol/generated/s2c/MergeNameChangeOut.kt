package zel.protocol.generated

data object MergeNameChangeOut

object MergeNameChangeOutCodec : OutgoingCodec<MergeNameChangeOut> {
    override val name = "MergeNameChangeOut"

    override fun read(r: PacketReader): MergeNameChangeOut = MergeNameChangeOut

    override fun write(w: PacketWriter, m: MergeNameChangeOut) = writeMergeNameChangeOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMergeNameChangeOut(w: PacketWriter, m: MergeNameChangeOut) {
    }
}
