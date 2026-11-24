package zel.protocol.generated

data class BreedPetsIn(
    val state: Int,
    val petOne: Int,
    val petTwo: Int,
)

object BreedPetsInCodec : IncomingCodec<BreedPetsIn> {
    override val name = "BreedPetsIn"

    override fun read(r: PacketReader): BreedPetsIn = readBreedPetsIn(r)

    override fun write(w: PacketWriter, m: BreedPetsIn) = writeBreedPetsIn(w, m)

    private fun readBreedPetsIn(r: PacketReader): BreedPetsIn {
        val vState = r.int()
        val vPetOne = r.int()
        val vPetTwo = r.int()
        return BreedPetsIn(vState, vPetOne, vPetTwo)
    }

    private fun writeBreedPetsIn(w: PacketWriter, m: BreedPetsIn) {
        w.int(m.state)
        w.int(m.petOne)
        w.int(m.petTwo)
    }
}
