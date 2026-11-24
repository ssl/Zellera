package zel.protocol.generated

data class DailyTasksTaskUpdateOut(
    val long1: Long,
    val int1: Int,
    val byte1: Byte,
    val int2: Int,
)

object DailyTasksTaskUpdateOutCodec : OutgoingCodec<DailyTasksTaskUpdateOut> {
    override val name = "DailyTasksTaskUpdateOut"

    override fun read(r: PacketReader): DailyTasksTaskUpdateOut = readDailyTasksTaskUpdateOut(r)

    override fun write(w: PacketWriter, m: DailyTasksTaskUpdateOut) = writeDailyTasksTaskUpdateOut(w, m)

    private fun readDailyTasksTaskUpdateOut(r: PacketReader): DailyTasksTaskUpdateOut {
        val vLong1 = r.long()
        val vInt1 = r.int()
        val vByte1 = r.byte()
        val vInt2 = r.int()
        return DailyTasksTaskUpdateOut(vLong1, vInt1, vByte1, vInt2)
    }

    private fun writeDailyTasksTaskUpdateOut(w: PacketWriter, m: DailyTasksTaskUpdateOut) {
        w.long(m.long1)
        w.int(m.int1)
        w.byte(m.byte1)
        w.int(m.int2)
    }
}
