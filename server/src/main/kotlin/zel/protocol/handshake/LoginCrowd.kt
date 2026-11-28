package zel.protocol.handshake

import zel.core.Configuration
import zel.game.players.DeviceReading
import zel.game.players.Player
import zel.net.server.proxy.ClientAddress
import zel.net.server.proxy.IpLiteral

class LoginCrowd(
    private val config: Configuration?,
) {
    fun refuses(
        playerId: Int,
        device: DeviceReading?,
        address: String,
        online: Collection<Player>,
    ): Boolean {
        val others = online.filter { it.profile.id != playerId }
        val perDevice = limit(DEVICE_KEY, PER_DEVICE)
        val deviceId = device?.usable("id")
        if (perDevice > 0 && deviceId != null && others.count { it.device?.usable("id") == deviceId } >= perDevice) {
            return true
        }
        val perAddress = limit(ADDRESS_KEY, PER_ADDRESS)
        if (perAddress <= 0 || IpLiteral.isLoopback(address)) return false
        return others.count { other -> other.client?.let { ClientAddress.of(it.channel) } == address } >= perAddress
    }

    private fun limit(
        key: String,
        default: Int,
    ): Int = config?.getInt(key, default) ?: default

    companion object {
        const val DEVICE_KEY = "login.players_per_device"
        const val ADDRESS_KEY = "login.players_per_address"
        const val PER_DEVICE = 3
        const val PER_ADDRESS = 5
    }
}
