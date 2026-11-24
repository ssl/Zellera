package zel.protocol.generated

data class ConfirmBreedingResultOut(
    val int1: Int,
    val int2: Int,
)

object ConfirmBreedingResultOutCodec : OutgoingCodec<ConfirmBreedingResultOut> {
    override val name = "ConfirmBreedingResultOut"

    override fun read(r: PacketReader): ConfirmBreedingResultOut = readConfirmBreedingResultOut(r)

    override fun write(w: PacketWriter, m: ConfirmBreedingResultOut) = writeConfirmBreedingResultOut(w, m)

    private fun readConfirmBreedingResultOut(r: PacketReader): ConfirmBreedingResultOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ConfirmBreedingResultOut(vInt1, vInt2)
    }

    private fun writeConfirmBreedingResultOut(w: PacketWriter, m: ConfirmBreedingResultOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
