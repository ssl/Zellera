package zel.protocol.generated

data class ClubGiftNotificationOut(
    val int1: Int,
)

object ClubGiftNotificationOutCodec : OutgoingCodec<ClubGiftNotificationOut> {
    override val name = "ClubGiftNotificationOut"

    override fun read(r: PacketReader): ClubGiftNotificationOut = readClubGiftNotificationOut(r)

    override fun write(w: PacketWriter, m: ClubGiftNotificationOut) = writeClubGiftNotificationOut(w, m)

    private fun readClubGiftNotificationOut(r: PacketReader): ClubGiftNotificationOut {
        val vInt1 = r.int()
        return ClubGiftNotificationOut(vInt1)
    }

    private fun writeClubGiftNotificationOut(w: PacketWriter, m: ClubGiftNotificationOut) {
        w.int(m.int1)
    }
}
