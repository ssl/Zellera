package zel.protocol.generated

data class VisitPlayerIn(
    val username: String,
)

object VisitPlayerInCodec : IncomingCodec<VisitPlayerIn> {
    override val name = "VisitPlayerIn"

    override fun read(r: PacketReader): VisitPlayerIn = readVisitPlayerIn(r)

    override fun write(w: PacketWriter, m: VisitPlayerIn) = writeVisitPlayerIn(w, m)

    private fun readVisitPlayerIn(r: PacketReader): VisitPlayerIn {
        val vUsername = r.string()
        return VisitPlayerIn(vUsername)
    }

    private fun writeVisitPlayerIn(w: PacketWriter, m: VisitPlayerIn) {
        w.string(m.username)
    }
}
