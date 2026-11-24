package zel.protocol.generated

data class AccountPreferencesOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val boolean1: Boolean,
    val boolean2: Boolean,
    val boolean3: Boolean,
    val int4: Int,
    val int5: Int,
    val boolean4: Boolean,
    val boolean5: Boolean,
    val boolean6: Boolean,
    val int6: Int,
    val boolean7: Boolean,
    val trailing1: Boolean?,
    val trailing2: String?,
    val trailing3: Int?,
    val trailing4: Int?,
    val trailing5: Int?,
    val trailing6: Int?,
    val trailing7: Int?,
)

object AccountPreferencesOutCodec : OutgoingCodec<AccountPreferencesOut> {
    override val name = "AccountPreferencesOut"

    override fun read(r: PacketReader): AccountPreferencesOut = readAccountPreferencesOut(r)

    override fun write(w: PacketWriter, m: AccountPreferencesOut) = writeAccountPreferencesOut(w, m)

    private fun readAccountPreferencesOut(r: PacketReader): AccountPreferencesOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vBoolean3 = r.boolean()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean4 = r.boolean()
        val vBoolean5 = r.boolean()
        val vBoolean6 = r.boolean()
        val vInt6 = r.int()
        val vBoolean7 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.boolean() else null
        val vTrailing2 = if (r.remaining > 0) r.string() else null
        val vTrailing3 = if (r.remaining > 0) r.int() else null
        val vTrailing4 = if (r.remaining > 0) r.int() else null
        val vTrailing5 = if (r.remaining > 0) r.int() else null
        val vTrailing6 = if (r.remaining > 0) r.int() else null
        val vTrailing7 = if (r.remaining > 0) r.int() else null
        return AccountPreferencesOut(vInt1, vInt2, vInt3, vBoolean1, vBoolean2, vBoolean3, vInt4, vInt5, vBoolean4, vBoolean5, vBoolean6, vInt6, vBoolean7, vTrailing1, vTrailing2, vTrailing3, vTrailing4, vTrailing5, vTrailing6, vTrailing7)
    }

    private fun writeAccountPreferencesOut(w: PacketWriter, m: AccountPreferencesOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.boolean(m.boolean3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean4)
        w.boolean(m.boolean5)
        w.boolean(m.boolean6)
        w.int(m.int6)
        w.boolean(m.boolean7)
        m.trailing1?.let { w.boolean(it) }
        m.trailing2?.let { w.string(it) }
        m.trailing3?.let { w.int(it) }
        m.trailing4?.let { w.int(it) }
        m.trailing5?.let { w.int(it) }
        m.trailing6?.let { w.int(it) }
        m.trailing7?.let { w.int(it) }
    }
}
