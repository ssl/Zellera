package zel.protocol.generated

data class BuildPassPlacementWarningOut(
    val rest: ByteArray,
)

object BuildPassPlacementWarningOutCodec : OutgoingCodec<BuildPassPlacementWarningOut> {
    override val name = "BuildPassPlacementWarningOut"

    override fun read(r: PacketReader): BuildPassPlacementWarningOut = readBuildPassPlacementWarningOut(r)

    override fun write(w: PacketWriter, m: BuildPassPlacementWarningOut) = writeBuildPassPlacementWarningOut(w, m)

    private fun readBuildPassPlacementWarningOut(r: PacketReader): BuildPassPlacementWarningOut {
        val vRest = r.rest()
        return BuildPassPlacementWarningOut(vRest)
    }

    private fun writeBuildPassPlacementWarningOut(w: PacketWriter, m: BuildPassPlacementWarningOut) {
        w.bytes(m.rest)
    }
}
