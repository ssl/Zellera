package zel.protocol.generated

data class GetSecondsUntilIn(
    val date: String,
)

object GetSecondsUntilInCodec : IncomingCodec<GetSecondsUntilIn> {
    override val name = "GetSecondsUntilIn"

    override fun read(r: PacketReader): GetSecondsUntilIn = readGetSecondsUntilIn(r)

    override fun write(w: PacketWriter, m: GetSecondsUntilIn) = writeGetSecondsUntilIn(w, m)

    private fun readGetSecondsUntilIn(r: PacketReader): GetSecondsUntilIn {
        val vDate = r.string()
        return GetSecondsUntilIn(vDate)
    }

    private fun writeGetSecondsUntilIn(w: PacketWriter, m: GetSecondsUntilIn) {
        w.string(m.date)
    }
}
