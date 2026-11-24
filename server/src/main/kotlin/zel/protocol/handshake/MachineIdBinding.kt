package zel.protocol.handshake

import zel.core.threading.GameThreads
import zel.game.moderation.ModerationService
import zel.game.players.DeviceReading
import zel.net.clients.ClientConnection
import zel.net.clients.ClientConnections

/** A connection's machine id is the id piece of the device it sent; none when it sent no device or cannot keep one. */
class MachineIdBinding(
    private val clients: ClientConnections,
    private val moderation: ModerationService,
    private val threading: GameThreads,
) {
    fun bind(
        client: ClientConnection,
        device: DeviceReading?,
    ): String {
        val machineId = device?.usable("id").orEmpty()
        client.machineId = machineId
        val player = client.player
        val profile = player?.profile
        if (profile != null && machineId.isNotEmpty()) {
            if (profile.machineId != machineId) {
                profile.machineId = machineId
                threading.run(player)
            }
            if (moderation.banning.hasMachineBan(client)) clients.close(client)
        }
        return machineId
    }
}
