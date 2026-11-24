package zel.protocol.generated

data class StartAddingToChestIn(
    val chestItemId: Int,
)

object StartAddingToChestInCodec : IncomingCodec<StartAddingToChestIn> {
    override val name = "StartAddingToChestIn"

    override fun read(r: PacketReader): StartAddingToChestIn = readStartAddingToChestIn(r)

    override fun write(w: PacketWriter, m: StartAddingToChestIn) = writeStartAddingToChestIn(w, m)

    private fun readStartAddingToChestIn(r: PacketReader): StartAddingToChestIn {
        val vChestItemId = r.int()
        return StartAddingToChestIn(vChestItemId)
    }

    private fun writeStartAddingToChestIn(w: PacketWriter, m: StartAddingToChestIn) {
        w.int(m.chestItemId)
    }
}
