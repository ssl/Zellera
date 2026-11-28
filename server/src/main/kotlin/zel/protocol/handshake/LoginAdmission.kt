package zel.protocol.handshake

import zel.core.Configuration
import zel.net.clients.ClientConnection
import zel.net.crypto.RsaKey

class LoginAdmission(
    private val config: Configuration,
    private val rsaKey: RsaKey?,
    private val gameReady: () -> Boolean,
    private val loadAllows: () -> Boolean,
) {
    enum class Refusal {
        CONNECTION_CLOSED,
        GAME_STARTING,
        PLAIN_STREAM,
        RELEASE,
        OVERLOADED,
    }

    fun refusal(client: ClientConnection): Refusal? =
        when {
            !client.channel.isActive -> Refusal.CONNECTION_CLOSED
            !gameReady() -> Refusal.GAME_STARTING
            encryptionRequired() && !client.streamEncrypted -> Refusal.PLAIN_STREAM
            !AllowedReleases(allowedReleases()).admits(client.releaseName) -> Refusal.RELEASE
            !servedBuild(client.releaseName) -> Refusal.RELEASE
            !loadAllows() -> Refusal.OVERLOADED
            else -> null
        }

    private fun encryptionRequired() = rsaKey != null && config.getBoolean(ENCRYPTION_FORCED)

    fun allowedReleases(): String = config.getValue(RELEASES_ALLOWED, AllowedReleases.ANY)

    private fun servedBuild(release: String): Boolean {
        val build = config.getValue(BUILD, "").trim()
        return build.isEmpty() || release == build || release.endsWith("-$build")
    }

    companion object {
        const val RELEASES_ALLOWED = "login.client_releases"
        const val ENCRYPTION_FORCED = "login.key_exchange.is_required"
        const val BUILD = "login.client_build"
    }
}
