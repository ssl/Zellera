package zel.protocol.generated

data class ClientHelloIn(
    val release: String,
    val clientType: String,
    val platform: Int,
    val deviceCategory: Int,
)

object ClientHelloInCodec : IncomingCodec<ClientHelloIn> {
    override val name = "ClientHelloIn"

    override fun read(r: PacketReader): ClientHelloIn = readClientHelloIn(r)

    override fun write(w: PacketWriter, m: ClientHelloIn) = writeClientHelloIn(w, m)

    private fun readClientHelloIn(r: PacketReader): ClientHelloIn {
        val vRelease = r.string()
        val vClientType = r.string()
        val vPlatform = r.int()
        val vDeviceCategory = r.int()
        return ClientHelloIn(vRelease, vClientType, vPlatform, vDeviceCategory)
    }

    private fun writeClientHelloIn(w: PacketWriter, m: ClientHelloIn) {
        w.string(m.release)
        w.string(m.clientType)
        w.int(m.platform)
        w.int(m.deviceCategory)
    }
}
