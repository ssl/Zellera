package zel.protocol.generated

data class GoToBreedingNestFailureOut(
    val int1: Int,
)

object GoToBreedingNestFailureOutCodec : OutgoingCodec<GoToBreedingNestFailureOut> {
    override val name = "GoToBreedingNestFailureOut"

    override fun read(r: PacketReader): GoToBreedingNestFailureOut = readGoToBreedingNestFailureOut(r)

    override fun write(w: PacketWriter, m: GoToBreedingNestFailureOut) = writeGoToBreedingNestFailureOut(w, m)

    private fun readGoToBreedingNestFailureOut(r: PacketReader): GoToBreedingNestFailureOut {
        val vInt1 = r.int()
        return GoToBreedingNestFailureOut(vInt1)
    }

    private fun writeGoToBreedingNestFailureOut(w: PacketWriter, m: GoToBreedingNestFailureOut) {
        w.int(m.int1)
    }
}
