package zel.protocol.generated

data object GetPlayerFlatCatsIn

object GetPlayerFlatCatsInCodec : IncomingCodec<GetPlayerFlatCatsIn> {
    override val name = "GetPlayerFlatCatsIn"

    override fun read(r: PacketReader): GetPlayerFlatCatsIn = GetPlayerFlatCatsIn

    override fun write(w: PacketWriter, m: GetPlayerFlatCatsIn) = writeGetPlayerFlatCatsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPlayerFlatCatsIn(w: PacketWriter, m: GetPlayerFlatCatsIn) {
    }
}
