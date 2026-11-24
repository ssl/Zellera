package zel.protocol.generated

data class CraftingResultOut(
    val boolean1: Boolean,
    val branch1: Branch1,
) {
    sealed interface Branch1 {
        data class Then(
            val string1: String,
            val string2: String,
            val string3: String,
        ) : Branch1
        data object Else : Branch1
    }
}

object CraftingResultOutCodec : OutgoingCodec<CraftingResultOut> {
    override val name = "CraftingResultOut"

    override fun read(r: PacketReader): CraftingResultOut = readCraftingResultOut(r)

    override fun write(w: PacketWriter, m: CraftingResultOut) = writeCraftingResultOut(w, m)

    private fun readCraftingResultOutBranch1Then(r: PacketReader): CraftingResultOut.Branch1.Then {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return CraftingResultOut.Branch1.Then(vString1, vString2, vString3)
    }

    private fun readCraftingResultOut(r: PacketReader): CraftingResultOut {
        val vBoolean1 = r.boolean()
        val vBranch1 = if (vBoolean1 == true) readCraftingResultOutBranch1Then(r) else CraftingResultOut.Branch1.Else
        return CraftingResultOut(vBoolean1, vBranch1)
    }

    private fun writeCraftingResultOutBranch1Then(w: PacketWriter, m: CraftingResultOut.Branch1.Then) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }

    private fun writeCraftingResultOut(w: PacketWriter, m: CraftingResultOut) {
        w.boolean(m.boolean1)
        when (val b = m.branch1) {
            is CraftingResultOut.Branch1.Then -> writeCraftingResultOutBranch1Then(w, b)
            is CraftingResultOut.Branch1.Else -> {}
        }
    }
}
