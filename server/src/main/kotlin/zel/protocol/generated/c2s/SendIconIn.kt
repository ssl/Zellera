package zel.protocol.generated

data class SendIconIn(
    val friendId: Int,
    val iconId: Int,
    val trackingId: Int,
)

object SendIconInCodec : IncomingCodec<SendIconIn> {
    override val name = "SendIconIn"

    override fun read(r: PacketReader): SendIconIn = readSendIconIn(r)

    override fun write(w: PacketWriter, m: SendIconIn) = writeSendIconIn(w, m)

    private fun readSendIconIn(r: PacketReader): SendIconIn {
        val vFriendId = r.int()
        val vIconId = r.int()
        val vTrackingId = r.int()
        return SendIconIn(vFriendId, vIconId, vTrackingId)
    }

    private fun writeSendIconIn(w: PacketWriter, m: SendIconIn) {
        w.int(m.friendId)
        w.int(m.iconId)
        w.int(m.trackingId)
    }
}
