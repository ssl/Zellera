package zel.protocol.generated

data class NestBreedingSuccessOut(
    val int1: Int,
    val int2: Int,
)

object NestBreedingSuccessOutCodec : OutgoingCodec<NestBreedingSuccessOut> {
    override val name = "NestBreedingSuccessOut"

    override fun read(r: PacketReader): NestBreedingSuccessOut = readNestBreedingSuccessOut(r)

    override fun write(w: PacketWriter, m: NestBreedingSuccessOut) = writeNestBreedingSuccessOut(w, m)

    private fun readNestBreedingSuccessOut(r: PacketReader): NestBreedingSuccessOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return NestBreedingSuccessOut(vInt1, vInt2)
    }

    private fun writeNestBreedingSuccessOut(w: PacketWriter, m: NestBreedingSuccessOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
