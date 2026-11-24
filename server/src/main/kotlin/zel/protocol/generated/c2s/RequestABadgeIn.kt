package zel.protocol.generated

data class RequestABadgeIn(
    val requestCode: String,
)

object RequestABadgeInCodec : IncomingCodec<RequestABadgeIn> {
    override val name = "RequestABadgeIn"

    override fun read(r: PacketReader): RequestABadgeIn = readRequestABadgeIn(r)

    override fun write(w: PacketWriter, m: RequestABadgeIn) = writeRequestABadgeIn(w, m)

    private fun readRequestABadgeIn(r: PacketReader): RequestABadgeIn {
        val vRequestCode = r.string()
        return RequestABadgeIn(vRequestCode)
    }

    private fun writeRequestABadgeIn(w: PacketWriter, m: RequestABadgeIn) {
        w.string(m.requestCode)
    }
}
