package zel.wired.usage

import zel.protocol.generated.WiredErrorLogsOut
import zel.protocol.generated.WiredErrorLogsOutCodec
import zel.protocol.generated.WiredRoomStatsOut
import zel.protocol.generated.WiredRoomStatsOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.form.WiredMasks

class UsageMonitor(
    private val usage: UsageGate,
    private val masks: (Room) -> WiredMasks,
    private val furniLimit: () -> Int,
    private val permanentVariables: (Room) -> Triple<Int, Int, Int> = { Triple(0, 0, 0) },
) {
    fun stats(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        if (!mayLook(scope.room, level)) return
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredRoomStatsOutCodec, stats(scope))
    }

    fun errors(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        if (!mayLook(scope.room, level)) return
        sendErrors(scope, playerId)
    }

    fun clear(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        if (!masks(scope.room).mayModify(level)) return
        usage.usage(scope.room).errors.clear()
        sendErrors(scope, playerId)
    }

    private fun mayLook(
        room: Room,
        level: Int,
    ): Boolean = masks(room).let { it.mayRead(level) || it.mayModify(level) }

    private fun stats(scope: RoomScope): WiredRoomStatsOut {
        val roomUsage = usage.usage(scope.room)
        val allowance = roomUsage.allowance
        val limit = roomUsage.meter.limit
        val reading = roomUsage.meter.reading(scope.now())
        val (wall, floor) =
            scope.room.furni
                .all()
                .partition { it.isWall }
        val furniMax = furniLimit()
        val permanent = permanentVariables(scope.room)
        return WiredRoomStatsOut(
            reading,
            limit,
            roomUsage.budget.heavy,
            floor.size,
            furniMax,
            wall.size,
            furniMax,
            permanent.first,
            allowance.furniVariables,
            permanent.second,
            allowance.userVariables,
            permanent.third,
            allowance.roomVariables,
        )
    }

    private fun sendErrors(
        scope: RoomScope,
        playerId: Int,
    ) {
        val table = usage.usage(scope.room).errors
        val rows =
            WiredError.entries.map {
                WiredErrorLogsOut.List1Item(it.wireId, it.name, it.category, table.count(it), table.latest(it))
            }
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredErrorLogsOutCodec, WiredErrorLogsOut(rows))
    }
}
