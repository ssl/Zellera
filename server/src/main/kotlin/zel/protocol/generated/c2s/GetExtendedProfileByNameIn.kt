package zel.protocol.generated

data class GetExtendedProfileByNameIn(
    val name: String,
)

object GetExtendedProfileByNameInCodec : IncomingCodec<GetExtendedProfileByNameIn> {
    override val name = "GetExtendedProfileByNameIn"

    override fun read(r: PacketReader): GetExtendedProfileByNameIn = readGetExtendedProfileByNameIn(r)

    override fun write(w: PacketWriter, m: GetExtendedProfileByNameIn) = writeGetExtendedProfileByNameIn(w, m)

    private fun readGetExtendedProfileByNameIn(r: PacketReader): GetExtendedProfileByNameIn {
        val vName = r.string()
        return GetExtendedProfileByNameIn(vName)
    }

    private fun writeGetExtendedProfileByNameIn(w: PacketWriter, m: GetExtendedProfileByNameIn) {
        w.string(m.name)
    }
}
