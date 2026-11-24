package zel.protocol.generated

data class IsBadgeRequestFulfilledOut(
    val requestCode: String,
    val fulfilled: Boolean,
)

object IsBadgeRequestFulfilledOutCodec : OutgoingCodec<IsBadgeRequestFulfilledOut> {
    override val name = "IsBadgeRequestFulfilledOut"

    override fun read(r: PacketReader): IsBadgeRequestFulfilledOut = readIsBadgeRequestFulfilledOut(r)

    override fun write(w: PacketWriter, m: IsBadgeRequestFulfilledOut) = writeIsBadgeRequestFulfilledOut(w, m)

    private fun readIsBadgeRequestFulfilledOut(r: PacketReader): IsBadgeRequestFulfilledOut {
        val vRequestCode = r.string()
        val vFulfilled = r.boolean()
        return IsBadgeRequestFulfilledOut(vRequestCode, vFulfilled)
    }

    private fun writeIsBadgeRequestFulfilledOut(w: PacketWriter, m: IsBadgeRequestFulfilledOut) {
        w.string(m.requestCode)
        w.boolean(m.fulfilled)
    }
}
