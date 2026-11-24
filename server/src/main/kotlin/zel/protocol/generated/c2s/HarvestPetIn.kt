package zel.protocol.generated

data class HarvestPetIn(
    val int1: Int,
)

object HarvestPetInCodec : IncomingCodec<HarvestPetIn> {
    override val name = "HarvestPetIn"

    override fun read(r: PacketReader): HarvestPetIn = readHarvestPetIn(r)

    override fun write(w: PacketWriter, m: HarvestPetIn) = writeHarvestPetIn(w, m)

    private fun readHarvestPetIn(r: PacketReader): HarvestPetIn {
        val vInt1 = r.int()
        return HarvestPetIn(vInt1)
    }

    private fun writeHarvestPetIn(w: PacketWriter, m: HarvestPetIn) {
        w.int(m.int1)
    }
}
