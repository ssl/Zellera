package zel.protocol.generated

data class VersionCheckIn(
    val int1: Int,
    val string1: String,
    val string2: String,
)

object VersionCheckInCodec : IncomingCodec<VersionCheckIn> {
    override val name = "VersionCheckIn"

    override fun read(r: PacketReader): VersionCheckIn = readVersionCheckIn(r)

    override fun write(w: PacketWriter, m: VersionCheckIn) = writeVersionCheckIn(w, m)

    private fun readVersionCheckIn(r: PacketReader): VersionCheckIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return VersionCheckIn(vInt1, vString1, vString2)
    }

    private fun writeVersionCheckIn(w: PacketWriter, m: VersionCheckIn) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }
}
