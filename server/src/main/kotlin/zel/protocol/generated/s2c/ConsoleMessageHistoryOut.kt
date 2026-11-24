package zel.protocol.generated

data class ConsoleMessageHistoryOut(
    val int1: Int,
    val rest: ByteArray,
)

object ConsoleMessageHistoryOutCodec : OutgoingCodec<ConsoleMessageHistoryOut> {
    override val name = "ConsoleMessageHistoryOut"

    override fun read(r: PacketReader): ConsoleMessageHistoryOut = readConsoleMessageHistoryOut(r)

    override fun write(w: PacketWriter, m: ConsoleMessageHistoryOut) = writeConsoleMessageHistoryOut(w, m)

    private fun readConsoleMessageHistoryOut(r: PacketReader): ConsoleMessageHistoryOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return ConsoleMessageHistoryOut(vInt1, vRest)
    }

    private fun writeConsoleMessageHistoryOut(w: PacketWriter, m: ConsoleMessageHistoryOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
