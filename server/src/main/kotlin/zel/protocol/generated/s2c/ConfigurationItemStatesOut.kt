package zel.protocol.generated

data class ConfigurationItemStatesOut(
    val boolean1: Boolean,
    val trailing1: Boolean?,
    val trailing2: Boolean?,
    val trailing3: Boolean?,
)

object ConfigurationItemStatesOutCodec : OutgoingCodec<ConfigurationItemStatesOut> {
    override val name = "ConfigurationItemStatesOut"

    override fun read(r: PacketReader): ConfigurationItemStatesOut = readConfigurationItemStatesOut(r)

    override fun write(w: PacketWriter, m: ConfigurationItemStatesOut) = writeConfigurationItemStatesOut(w, m)

    private fun readConfigurationItemStatesOut(r: PacketReader): ConfigurationItemStatesOut {
        val vBoolean1 = r.boolean()
        val vTrailing1 = if (r.remaining > 0) r.boolean() else null
        val vTrailing2 = if (r.remaining > 0) r.boolean() else null
        val vTrailing3 = if (r.remaining > 0) r.boolean() else null
        return ConfigurationItemStatesOut(vBoolean1, vTrailing1, vTrailing2, vTrailing3)
    }

    private fun writeConfigurationItemStatesOut(w: PacketWriter, m: ConfigurationItemStatesOut) {
        w.boolean(m.boolean1)
        m.trailing1?.let { w.boolean(it) }
        m.trailing2?.let { w.boolean(it) }
        m.trailing3?.let { w.boolean(it) }
    }
}
