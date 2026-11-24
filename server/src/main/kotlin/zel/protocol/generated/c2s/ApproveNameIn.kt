package zel.protocol.generated

data class ApproveNameIn(
    val petName: String,
    val int1: Int,
)

object ApproveNameInCodec : IncomingCodec<ApproveNameIn> {
    override val name = "ApproveNameIn"

    override fun read(r: PacketReader): ApproveNameIn = readApproveNameIn(r)

    override fun write(w: PacketWriter, m: ApproveNameIn) = writeApproveNameIn(w, m)

    private fun readApproveNameIn(r: PacketReader): ApproveNameIn {
        val vPetName = r.string()
        val vInt1 = r.int()
        return ApproveNameIn(vPetName, vInt1)
    }

    private fun writeApproveNameIn(w: PacketWriter, m: ApproveNameIn) {
        w.string(m.petName)
        w.int(m.int1)
    }
}
