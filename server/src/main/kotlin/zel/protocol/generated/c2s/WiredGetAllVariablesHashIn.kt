package zel.protocol.generated

data object WiredGetAllVariablesHashIn

object WiredGetAllVariablesHashInCodec : IncomingCodec<WiredGetAllVariablesHashIn> {
    override val name = "WiredGetAllVariablesHashIn"

    override fun read(r: PacketReader): WiredGetAllVariablesHashIn = WiredGetAllVariablesHashIn

    override fun write(w: PacketWriter, m: WiredGetAllVariablesHashIn) = writeWiredGetAllVariablesHashIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredGetAllVariablesHashIn(w: PacketWriter, m: WiredGetAllVariablesHashIn) {
    }
}
