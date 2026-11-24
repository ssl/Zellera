package zel.protocol.generated

data object BuildPassQueryFurniCountIn

object BuildPassQueryFurniCountInCodec : IncomingCodec<BuildPassQueryFurniCountIn> {
    override val name = "BuildPassQueryFurniCountIn"

    override fun read(r: PacketReader): BuildPassQueryFurniCountIn = BuildPassQueryFurniCountIn

    override fun write(w: PacketWriter, m: BuildPassQueryFurniCountIn) = writeBuildPassQueryFurniCountIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeBuildPassQueryFurniCountIn(w: PacketWriter, m: BuildPassQueryFurniCountIn) {
    }
}
