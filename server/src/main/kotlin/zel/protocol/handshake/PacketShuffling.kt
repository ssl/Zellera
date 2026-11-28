package zel.protocol.handshake

import zel.core.Configuration
import zel.net.clients.ClientConnection
import zel.net.server.PacketShuffle
import zel.net.server.ShuffleStage
import zel.net.server.proxy.ClientAddress
import zel.security.Suspicion
import zel.security.SuspicionLog

class PacketShuffling(
    private val config: Configuration,
    private val modulusHex: String?,
    private val suspicions: SuspicionLog,
) {
    fun stageFor(
        client: ClientConnection,
        sharedKey: ByteArray,
    ): ShuffleStage? {
        val modulus = modulusHex?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!config.getBoolean(ON_KEY, false)) return null
        val shuffle = PacketShuffle.of(sharedKey, client.releaseName, modulus)
        return ShuffleStage(shuffle, config.getInt(STRIKES_KEY, STRIKES)) { wire, strikes ->
            if (strikes == 1) {
                suspicions.record(
                    client.player?.profile?.id,
                    ClientAddress.of(client.channel),
                    client.device?.usable("id"),
                    Suspicion.UNKNOWN_ID,
                    "wire id $wire",
                )
            }
        }
    }

    companion object {
        const val ON_KEY = "login.packet_shuffle.is_on"
        const val STRIKES_KEY = "login.packet_shuffle.strikes"
        const val STRIKES = 3
    }
}
