package zel.protocol.generated

data class BuildPassFurniCountOut(
    val int1: Int,
)

object BuildPassFurniCountOutCodec : OutgoingCodec<BuildPassFurniCountOut> {
    override val name = "BuildPassFurniCountOut"

    override fun read(r: PacketReader): BuildPassFurniCountOut = readBuildPassFurniCountOut(r)

    override fun write(w: PacketWriter, m: BuildPassFurniCountOut) = writeBuildPassFurniCountOut(w, m)

    private fun readBuildPassFurniCountOut(r: PacketReader): BuildPassFurniCountOut {
        val vInt1 = r.int()
        return BuildPassFurniCountOut(vInt1)
    }

    private fun writeBuildPassFurniCountOut(w: PacketWriter, m: BuildPassFurniCountOut) {
        w.int(m.int1)
    }
}
