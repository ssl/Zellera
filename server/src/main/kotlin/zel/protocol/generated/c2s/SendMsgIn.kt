package zel.protocol.generated

data class SendMsgIn(
    val userId: Int,
    val message: String,
    val confirmationId: Int,
)

object SendMsgInCodec : IncomingCodec<SendMsgIn> {
    override val name = "SendMsgIn"

    override fun read(r: PacketReader): SendMsgIn = readSendMsgIn(r)

    override fun write(w: PacketWriter, m: SendMsgIn) = writeSendMsgIn(w, m)

    private fun readSendMsgIn(r: PacketReader): SendMsgIn {
        val vUserId = r.int()
        val vMessage = r.string()
        val vConfirmationId = r.int()
        return SendMsgIn(vUserId, vMessage, vConfirmationId)
    }

    private fun writeSendMsgIn(w: PacketWriter, m: SendMsgIn) {
        w.int(m.userId)
        w.string(m.message)
        w.int(m.confirmationId)
    }
}
