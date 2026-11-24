package zel.protocol.resolution

import zel.game.achievements.resolution.ResolutionState
import zel.game.achievements.resolution.Resolutions
import zel.net.clients.ClientConnection
import zel.protocol.generated.GetResolutionAchievementsInCodec
import zel.protocol.generated.ResetResolutionAchievementInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.room.port.PlacedFurni

class ResolutionPackets(
    private val resolutions: Resolutions,
    private val now: () -> Int,
) : PacketPackage {
    override val name = "resolution"

    private val messages = ResolutionMessages()
    private val openLimit = ClientCooldown("resolution.open", OPEN_INTERVAL_MS)
    private val resetLimit = ClientCooldown("resolution.reset", RESET_INTERVAL_MS)

    override val routes =
        listOf(
            PacketRoute(GetResolutionAchievementsInCodec) { session, m ->
                if (openLimit.acquire(session.client)) {
                    open(session.client, itemId = m.itemId, achievementId = m.achievementId)
                }
            },
            PacketRoute(ResetResolutionAchievementInCodec) { session, m ->
                if (resetLimit.acquire(session.client)) reset(session.client, itemId = m.itemId)
            },
        )

    private fun open(
        client: ClientConnection,
        itemId: Int,
        achievementId: Int,
    ) {
        val item = ownItem(client, itemId) ?: return
        val player = client.player ?: return
        when (val state = resolutions.open(player, itemId, achievementId, now())) {
            is ResolutionState.Picking -> {
                messages.picker(client, itemId, state.choices, state.durationSeconds)
            }

            is ResolutionState.Completed -> {
                messages.completed(client, item.definition.name, state.badge)
            }

            is ResolutionState.Running -> {
                messages.progress(
                    client,
                    itemId,
                    state.resolution.achievement,
                    state.badge,
                    state.progress.current,
                    state.progress.required,
                    state.secondsLeft,
                )
            }
        }
    }

    private fun reset(
        client: ClientConnection,
        itemId: Int,
    ) {
        ownItem(client, itemId) ?: return
        resolutions.reset(itemId)
    }

    private fun ownItem(
        client: ClientConnection,
        itemId: Int,
    ): PlacedFurni? {
        val player = client.player ?: return null
        val roomId = player.rooms.roomOf(player.profile.id) ?: return null
        return player.rooms.furniIn(roomId, itemId)?.takeIf { it.ownerId == player.profile.id }
    }

    private companion object {
        const val OPEN_INTERVAL_MS = 500L
        const val RESET_INTERVAL_MS = 1000L
    }
}
