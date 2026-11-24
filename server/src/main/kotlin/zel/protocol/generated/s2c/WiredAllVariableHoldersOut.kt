package zel.protocol.generated

data class WiredAllVariableHoldersOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val string2: String,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val boolean4: Boolean,
    val boolean5: Boolean,
    val boolean6: Boolean,
    val boolean7: Boolean,
    val boolean8: Boolean,
    val boolean9: Boolean,
    val boolean10: Boolean,
    val rest: ByteArray,
)

object WiredAllVariableHoldersOutCodec : OutgoingCodec<WiredAllVariableHoldersOut> {
    override val name = "WiredAllVariableHoldersOut"

    override fun read(r: PacketReader): WiredAllVariableHoldersOut = readWiredAllVariableHoldersOut(r)

    override fun write(w: PacketWriter, m: WiredAllVariableHoldersOut) = writeWiredAllVariableHoldersOut(w, m)

    private fun readWiredAllVariableHoldersOut(r: PacketReader): WiredAllVariableHoldersOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vString2 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vBoolean4 = r.boolean()
        val vBoolean5 = r.boolean()
        val vBoolean6 = r.boolean()
        val vBoolean7 = r.boolean()
        val vBoolean8 = r.boolean()
        val vBoolean9 = r.boolean()
        val vBoolean10 = r.boolean()
        val vRest = r.rest()
        return WiredAllVariableHoldersOut(vInt1, vString1, vInt2, vString2, vInt3, vInt4, vBoolean1, vBoolean2, vBoolean3, vBoolean4, vBoolean5, vBoolean6, vBoolean7, vBoolean8, vBoolean9, vBoolean10, vRest)
    }

    private fun writeWiredAllVariableHoldersOut(w: PacketWriter, m: WiredAllVariableHoldersOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.string(m.string2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.boolean(m.boolean4)
        w.boolean(m.boolean5)
        w.boolean(m.boolean6)
        w.boolean(m.boolean7)
        w.boolean(m.boolean8)
        w.boolean(m.boolean9)
        w.boolean(m.boolean10)
        w.bytes(m.rest)
    }
}
