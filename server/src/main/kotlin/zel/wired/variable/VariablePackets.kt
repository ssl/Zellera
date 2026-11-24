package zel.wired.variable

import zel.protocol.generated.WiredDeleteAllVariableHoldersInCodec
import zel.protocol.generated.WiredGetAllVariableHoldersInCodec
import zel.protocol.generated.WiredGetAllVariablesDiffsInCodec
import zel.protocol.generated.WiredGetAllVariablesHashInCodec
import zel.protocol.generated.WiredGetPlayerPermanentVariablesInCodec
import zel.protocol.generated.WiredGetVariableOwnersPageInCodec
import zel.protocol.generated.WiredGetVariablesForObjectInCodec
import zel.protocol.generated.WiredSetObjectVariableValueInCodec
import zel.protocol.generated.WiredSetPlayerPermanentVariableInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.wired.protocol.withWiredLevel

class VariablePackets(
    private val registry: RoomRegistry,
    menu: VariableMenu,
    private val levels: (Room) -> Map<Int, Int>,
) : PacketPackage {
    override val name = "wired-variables"
    override val routes =
        listOf(
            PacketRoute(WiredGetAllVariablesHashInCodec) { session, _ -> inRoom(session, menu::hash) },
            PacketRoute(WiredGetAllVariablesDiffsInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> menu.diffs(scope, playerId, level, m.hashes) }
            },
            PacketRoute(WiredGetVariablesForObjectInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> menu.inspect(scope, playerId, level, m.type, m.id) }
            },
            PacketRoute(WiredSetObjectVariableValueInCodec) { session, m ->
                val write = ObjectWrite(m.airTarget, m.objectId, m.variableId, m.int1, m.action)
                inRoom(session) { scope, playerId, level -> menu.set(scope, playerId, level, write) }
            },
            PacketRoute(WiredGetAllVariableHoldersInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> menu.holders(scope, playerId, level, m.variableId) }
            },
            PacketRoute(WiredGetVariableOwnersPageInCodec) { session, m ->
                val page = HoldersPage(m.variableId, m.page, m.pageSize, m.userTypeFilter, m.sortTypeFilter)
                inRoom(session) { scope, playerId, level -> menu.page(scope, playerId, level, page) }
            },
            PacketRoute(WiredDeleteAllVariableHoldersInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> menu.deleteAll(scope, playerId, level, m.variableId) }
            },
            PacketRoute(WiredGetPlayerPermanentVariablesInCodec) { session, m ->
                inRoom(session) { scope, playerId, level ->
                    menu.permanent(scope, playerId, level, m.entityType, m.entityId)
                }
            },
            PacketRoute(WiredSetPlayerPermanentVariableInCodec) { session, m ->
                val write = PermanentWrite(m.entityType, m.userId, m.variableId, m.int1, m.action)
                inRoom(session) { scope, playerId, level -> menu.setPermanent(scope, playerId, level, write) }
            },
        )

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, Int) -> Unit,
    ) = registry.withWiredLevel(session, levels, block)
}
