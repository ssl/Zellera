package zel.protocol.generated

data object GetDailyTasksIn

object GetDailyTasksInCodec : IncomingCodec<GetDailyTasksIn> {
    override val name = "GetDailyTasksIn"

    override fun read(r: PacketReader): GetDailyTasksIn = GetDailyTasksIn

    override fun write(w: PacketWriter, m: GetDailyTasksIn) = writeGetDailyTasksIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetDailyTasksIn(w: PacketWriter, m: GetDailyTasksIn) {
    }
}
