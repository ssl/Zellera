package zel.wired.protocol

import zel.protocol.generated.ApplySnapshotInCodec
import zel.protocol.generated.OpenInCodec
import zel.protocol.generated.UpdateActionInCodec
import zel.protocol.generated.UpdateAddonInCodec
import zel.protocol.generated.UpdateConditionInCodec
import zel.protocol.generated.UpdateSelectorInCodec
import zel.protocol.generated.UpdateTriggerInCodec
import zel.protocol.generated.UpdateVariableInCodec
import zel.protocol.generated.WiredGetRoomLogsInCodec
import zel.protocol.generated.WiredGetRoomSettingsInCodec
import zel.protocol.generated.WiredSetRoomSettingsInCodec
import zel.protocol.generated.WiredUpdateRoomInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.wired.box.BoxKind
import zel.wired.effect.furni.ApplySnapshots
import zel.wired.form.SaveRequest

class WiredFormPackets(
    private val registry: RoomRegistry,
    private val forms: WiredForms,
    masks: WiredMaskMenu,
    private val levels: (Room) -> Map<Int, Int>,
    reloads: RoomReloads,
    snapshots: ApplySnapshots,
    maxDelay: () -> Int = { DEFAULT_MAX_DELAY },
) : PacketPackage {
    override val name = "wired-forms"
    override val routes =
        listOf(
            PacketRoute(ApplySnapshotInCodec) { session, m ->
                inRoom(session) { scope, _, level -> snapshots.apply(scope, level, m.itemId) }
            },
            PacketRoute(OpenInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> forms.open(scope, playerId, level, m.itemId) }
            },
            PacketRoute(UpdateTriggerInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Trigger,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                    ),
                )
            },
            PacketRoute(UpdateActionInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Effect,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                        // delay and quantifier are stored as tinyints: memory keeps what a reload reads back
                        delay = m.delay.coerceIn(0, minOf(maxDelay(), Byte.MAX_VALUE.toInt())),
                    ),
                )
            },
            PacketRoute(UpdateConditionInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Condition,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                        quantifier = m.quantifier.coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()),
                    ),
                )
            },
            PacketRoute(UpdateSelectorInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Selector,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                        filter = m.filter,
                        invert = m.invert,
                    ),
                )
            },
            PacketRoute(UpdateAddonInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Addon,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                    ),
                )
            },
            PacketRoute(UpdateVariableInCodec) { session, m ->
                save(
                    session,
                    BoxKind.Variable,
                    SaveRequest(
                        m.id,
                        m.intParams,
                        m.stringParam,
                        m.stuffIds,
                        m.stuffIds2,
                        m.furniSources,
                        m.userSources,
                        m.variableIds,
                    ),
                )
            },
            PacketRoute(WiredGetRoomSettingsInCodec) { session, _ ->
                inRoom(session) { scope, playerId, level -> masks.show(scope, playerId, level) }
            },
            PacketRoute(WiredSetRoomSettingsInCodec) { session, m ->
                inRoom(session) { scope, _, level ->
                    masks.change(scope, level, m.modifyMask, m.inspectMask, m.timezone, levels(scope.room))
                }
            },
            PacketRoute(WiredGetRoomLogsInCodec) { session, m ->
                inRoom(session) { scope, playerId, level -> masks.logs(scope, playerId, level, m) }
            },
            PacketRoute(WiredUpdateRoomInCodec) { session, m ->
                inRoom(session) { scope, _, level -> reloads.request(scope, level, m.rollback) }
            },
        )

    private fun save(
        session: PacketSession,
        kind: BoxKind,
        request: SaveRequest,
    ) = inRoom(session) { scope, playerId, level -> forms.save(scope, playerId, level, kind, request) }

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, Int) -> Unit,
    ) = registry.withWiredLevel(session, levels, block)

    companion object {
        const val DEFAULT_MAX_DELAY = 20
    }
}
