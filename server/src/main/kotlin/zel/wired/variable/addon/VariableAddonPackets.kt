package zel.wired.variable.addon

import zel.core.rules.rules
import zel.protocol.generated.SynchronizeVariableAddonInCodec
import zel.protocol.generated.WiredGenerateWebApiKeyInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.wired.protocol.withWiredLevel
import io.netty.channel.ChannelHandler

class VariableAddonPackets(
    private val registry: RoomRegistry,
    sync: ShareSync,
    webApi: WebApiBoxes,
    private val levels: (Room) -> Map<Int, Int>,
    private val bridge: () -> ChannelHandler,
) : PacketPackage {
    private val syncGap = ClientCooldown(SYNC_GROUP, rules.rates.shareSyncGapMillis)

    override val name = "wired-variable-addons"
    override val routes =
        listOf(
            PacketRoute(SynchronizeVariableAddonInCodec) { session, m ->
                if (!syncGap.acquire(session.client)) return@PacketRoute
                inRoom(session) { scope, playerId, level -> sync.synchronize(scope, playerId, level, m.furniId) }
            },
            PacketRoute(WiredGenerateWebApiKeyInCodec) { session, m ->
                inRoom(session) { scope, playerId, _ -> webApi.generate(scope, playerId, m.itemId, m.isReadKey) }
            },
        )

    fun bridgeHandler(): ChannelHandler = bridge()

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, Int) -> Unit,
    ) = registry.withWiredLevel(session, levels, block)

    private companion object {
        const val SYNC_GROUP = "wired.share_sync"
    }
}
