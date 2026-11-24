package zel.wired.usage

import zel.protocol.generated.WiredClearErrorLogsInCodec
import zel.protocol.generated.WiredGetErrorLogsInCodec
import zel.protocol.generated.WiredGetRoomStatsInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.wired.protocol.withWiredLevel

class UsagePackets(
    private val registry: RoomRegistry,
    monitor: UsageMonitor,
    private val levels: (Room) -> Map<Int, Int>,
) : PacketPackage {
    override val name = "wired-monitor"
    override val routes =
        listOf(
            PacketRoute(WiredGetRoomStatsInCodec) { session, _ -> inRoom(session, monitor::stats) },
            PacketRoute(WiredGetErrorLogsInCodec) { session, _ -> inRoom(session, monitor::errors) },
            PacketRoute(WiredClearErrorLogsInCodec) { session, _ -> inRoom(session, monitor::clear) },
        )

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, Int) -> Unit,
    ) = registry.withWiredLevel(session, levels, block)
}
