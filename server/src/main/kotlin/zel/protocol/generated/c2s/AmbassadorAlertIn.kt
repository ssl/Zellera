package zel.protocol.generated

data class AmbassadorAlertIn(
    val userId: Int,
)

object AmbassadorAlertInCodec : IncomingCodec<AmbassadorAlertIn> {
    override val name = "AmbassadorAlertIn"

    override fun read(r: PacketReader): AmbassadorAlertIn = readAmbassadorAlertIn(r)

    override fun write(w: PacketWriter, m: AmbassadorAlertIn) = writeAmbassadorAlertIn(w, m)

    private fun readAmbassadorAlertIn(r: PacketReader): AmbassadorAlertIn {
        val vUserId = r.int()
        return AmbassadorAlertIn(vUserId)
    }

    private fun writeAmbassadorAlertIn(w: PacketWriter, m: AmbassadorAlertIn) {
        w.int(m.userId)
    }
}
