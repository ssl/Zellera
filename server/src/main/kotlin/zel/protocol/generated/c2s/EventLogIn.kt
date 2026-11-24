package zel.protocol.generated

data class EventLogIn(
    val category: String,
    val eventType: String,
    val action: String,
    val extraString: String,
    val extraInt: Int,
)

object EventLogInCodec : IncomingCodec<EventLogIn> {
    override val name = "EventLogIn"

    override fun read(r: PacketReader): EventLogIn = readEventLogIn(r)

    override fun write(w: PacketWriter, m: EventLogIn) = writeEventLogIn(w, m)

    private fun readEventLogIn(r: PacketReader): EventLogIn {
        val vCategory = r.string()
        val vEventType = r.string()
        val vAction = r.string()
        val vExtraString = r.string()
        val vExtraInt = r.int()
        return EventLogIn(vCategory, vEventType, vAction, vExtraString, vExtraInt)
    }

    private fun writeEventLogIn(w: PacketWriter, m: EventLogIn) {
        w.string(m.category)
        w.string(m.eventType)
        w.string(m.action)
        w.string(m.extraString)
        w.int(m.extraInt)
    }
}
