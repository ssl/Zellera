package zel.protocol.generated

data object GetJukeboxPlayListIn

object GetJukeboxPlayListInCodec : IncomingCodec<GetJukeboxPlayListIn> {
    override val name = "GetJukeboxPlayListIn"

    override fun read(r: PacketReader): GetJukeboxPlayListIn = GetJukeboxPlayListIn

    override fun write(w: PacketWriter, m: GetJukeboxPlayListIn) = writeGetJukeboxPlayListIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetJukeboxPlayListIn(w: PacketWriter, m: GetJukeboxPlayListIn) {
    }
}
