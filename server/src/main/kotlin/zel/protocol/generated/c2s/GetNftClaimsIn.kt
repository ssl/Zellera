package zel.protocol.generated

data class GetNftClaimsIn(
    val string1: String,
)

object GetNftClaimsInCodec : IncomingCodec<GetNftClaimsIn> {
    override val name = "GetNftClaimsIn"

    override fun read(r: PacketReader): GetNftClaimsIn = readGetNftClaimsIn(r)

    override fun write(w: PacketWriter, m: GetNftClaimsIn) = writeGetNftClaimsIn(w, m)

    private fun readGetNftClaimsIn(r: PacketReader): GetNftClaimsIn {
        val vString1 = r.string()
        return GetNftClaimsIn(vString1)
    }

    private fun writeGetNftClaimsIn(w: PacketWriter, m: GetNftClaimsIn) {
        w.string(m.string1)
    }
}
