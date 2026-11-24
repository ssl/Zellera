package zel.protocol.typed

class IncomingPacket(
    val name: String,
) {
    val secret: Boolean get() = name in SECRET_PACKETS

    companion object {
        val SECRET_PACKETS: Set<String> =
            setOf(
                "SSOTicketIn",
                "InitDiffieHandshakeIn",
                "CompleteDiffieHandshakeIn",
                "UniqueIDIn",
                "ChangeEmailIn",
                "VerifyCodeIn",
                "OpenFlatConnectionIn",
                "SaveRoomSettingsIn",
                "WiredGenerateWebApiKeyIn",
            )

        val NONE = IncomingPacket("no packet")
    }
}
