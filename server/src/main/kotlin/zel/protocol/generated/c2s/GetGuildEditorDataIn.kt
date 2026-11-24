package zel.protocol.generated

data object GetGuildEditorDataIn

object GetGuildEditorDataInCodec : IncomingCodec<GetGuildEditorDataIn> {
    override val name = "GetGuildEditorDataIn"

    override fun read(r: PacketReader): GetGuildEditorDataIn = GetGuildEditorDataIn

    override fun write(w: PacketWriter, m: GetGuildEditorDataIn) = writeGetGuildEditorDataIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGuildEditorDataIn(w: PacketWriter, m: GetGuildEditorDataIn) {
    }
}
