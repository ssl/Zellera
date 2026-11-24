package zel.protocol.generated

data object MyFrequentRoomHistorySearchIn

object MyFrequentRoomHistorySearchInCodec : IncomingCodec<MyFrequentRoomHistorySearchIn> {
    override val name = "MyFrequentRoomHistorySearchIn"

    override fun read(r: PacketReader): MyFrequentRoomHistorySearchIn = MyFrequentRoomHistorySearchIn

    override fun write(w: PacketWriter, m: MyFrequentRoomHistorySearchIn) = writeMyFrequentRoomHistorySearchIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMyFrequentRoomHistorySearchIn(w: PacketWriter, m: MyFrequentRoomHistorySearchIn) {
    }
}
