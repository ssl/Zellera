package zel.protocol.generated

data object RestoreClientOut

object RestoreClientOutCodec : OutgoingCodec<RestoreClientOut> {
    override val name = "RestoreClientOut"

    override fun read(r: PacketReader): RestoreClientOut = RestoreClientOut

    override fun write(w: PacketWriter, m: RestoreClientOut) = writeRestoreClientOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRestoreClientOut(w: PacketWriter, m: RestoreClientOut) {
    }
}
