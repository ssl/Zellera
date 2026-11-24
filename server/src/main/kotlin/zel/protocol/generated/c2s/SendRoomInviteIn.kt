package zel.protocol.generated

data class SendRoomInviteIn(
    val userIds: List<Int>,
    val message: String,
)

object SendRoomInviteInCodec : IncomingCodec<SendRoomInviteIn> {
    override val name = "SendRoomInviteIn"

    override fun read(r: PacketReader): SendRoomInviteIn = readSendRoomInviteIn(r)

    override fun write(w: PacketWriter, m: SendRoomInviteIn) = writeSendRoomInviteIn(w, m)

    private fun readSendRoomInviteIn(r: PacketReader): SendRoomInviteIn {
        val vUserIds = List(r.countInt(4)) { r.int() }
        val vMessage = r.string()
        return SendRoomInviteIn(vUserIds, vMessage)
    }

    private fun writeSendRoomInviteIn(w: PacketWriter, m: SendRoomInviteIn) {
        w.countInt(m.userIds.size)
        m.userIds.forEach { w.int(it) }
        w.string(m.message)
    }
}
