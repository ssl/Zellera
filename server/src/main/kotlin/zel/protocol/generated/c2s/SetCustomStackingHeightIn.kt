package zel.protocol.generated

data object SetCustomStackingHeightIn

object SetCustomStackingHeightInCodec : IncomingCodec<SetCustomStackingHeightIn> {
    override val name = "SetCustomStackingHeightIn"

    override fun read(r: PacketReader): SetCustomStackingHeightIn = SetCustomStackingHeightIn

    override fun write(w: PacketWriter, m: SetCustomStackingHeightIn) = writeSetCustomStackingHeightIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeSetCustomStackingHeightIn(w: PacketWriter, m: SetCustomStackingHeightIn) {
    }
}
