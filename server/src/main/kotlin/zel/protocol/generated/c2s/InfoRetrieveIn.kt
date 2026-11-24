package zel.protocol.generated

data object InfoRetrieveIn

object InfoRetrieveInCodec : IncomingCodec<InfoRetrieveIn> {
    override val name = "InfoRetrieveIn"

    override fun read(r: PacketReader): InfoRetrieveIn = InfoRetrieveIn

    override fun write(w: PacketWriter, m: InfoRetrieveIn) = writeInfoRetrieveIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeInfoRetrieveIn(w: PacketWriter, m: InfoRetrieveIn) {
    }
}
