package zel.protocol.generated

data class GetIsBadgeRequestFulfilledIn(
    val requestCode: String,
)

object GetIsBadgeRequestFulfilledInCodec : IncomingCodec<GetIsBadgeRequestFulfilledIn> {
    override val name = "GetIsBadgeRequestFulfilledIn"

    override fun read(r: PacketReader): GetIsBadgeRequestFulfilledIn = readGetIsBadgeRequestFulfilledIn(r)

    override fun write(w: PacketWriter, m: GetIsBadgeRequestFulfilledIn) = writeGetIsBadgeRequestFulfilledIn(w, m)

    private fun readGetIsBadgeRequestFulfilledIn(r: PacketReader): GetIsBadgeRequestFulfilledIn {
        val vRequestCode = r.string()
        return GetIsBadgeRequestFulfilledIn(vRequestCode)
    }

    private fun writeGetIsBadgeRequestFulfilledIn(w: PacketWriter, m: GetIsBadgeRequestFulfilledIn) {
        w.string(m.requestCode)
    }
}
