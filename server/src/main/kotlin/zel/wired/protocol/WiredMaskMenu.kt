package zel.wired.protocol

import zel.protocol.generated.FurnitureLabelsAndRegionsOutCodec
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredGetRoomLogsIn
import zel.protocol.generated.WiredMenuErrorOut
import zel.protocol.generated.WiredMenuErrorOutCodec
import zel.protocol.generated.WiredPermissionsOut
import zel.protocol.generated.WiredPermissionsOutCodec
import zel.protocol.generated.WiredRoomLogsOut
import zel.protocol.generated.WiredRoomLogsOutCodec
import zel.protocol.generated.WiredRoomSettingsOut
import zel.protocol.generated.WiredRoomSettingsOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone
import zel.wired.form.WiredMasks
import zel.wired.store.RoomBoxForms
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WiredMaskMenu(
    private val forms: RoomBoxForms,
    private val views: EntryViews = EntryViews.None,
) {
    fun show(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        val masks = forms.masks(scope.room)
        if (!masks.mayRead(level)) return
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredRoomSettingsOutCodec, settings(masks))
    }

    @Suppress("LongParameterList") // the message's three fields and the room's players
    fun change(
        scope: RoomScope,
        level: Int,
        modify: Int,
        read: Int,
        timezone: String,
        levels: Map<Int, Int>,
    ) {
        if (level < OWNER) return
        val before = forms.masks(scope.room)
        val masks = WiredMasks(modify, read, if (isZone(timezone)) timezone else before.timezone)
        forms.saveMasks(scope.room, masks)
        if (masks.modify == before.modify && masks.read == before.read) return
        levels.forEach { (id, lv) ->
            val permissions = WiredPermissionsOut(masks.mayModify(lv), masks.mayRead(lv))
            scope.out.send(Lane.Now, Audience.Player(id), WiredPermissionsOutCodec, permissions)
        }
        levels.forEach { (id, lv) ->
            scope.out.send(Lane.Now, Audience.Player(id), FurnitureLabelsAndRegionsOutCodec, views.labels(scope, lv))
        }
    }

    /** Anything but a known zone id keeps the room's zone. */
    private fun isZone(timezone: String): Boolean {
        if (timezone.isEmpty() || timezone.length > MAX_TIMEZONE) return false
        return try {
            ZoneId.of(timezone)
            true
        } catch (_: DateTimeException) {
            false
        }
    }

    fun logs(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        request: WiredGetRoomLogsIn,
    ) {
        if (!forms.masks(scope.room).mayRead(level)) return
        val size = request.pageSize
        val page = request.page
        if (page < 1 || size !in 1..MAX_LOG_PAGE) {
            scope.out.send(Lane.Now, Audience.Player(playerId), WiredMenuErrorOutCodec, WiredMenuErrorOut(BAD_PAGE))
            return
        }
        val zone = forms.masks(scope.room).timezone
        val shown = if (zone.isEmpty()) LOG_TIME else LOG_TIME.withZone(ZoneId.of(zone))
        val found =
            forms
                .log(scope.room)
                .page(page, size, request.logLevelFilter, request.logSourceFilter, request.query)
        val event =
            WiredRoomLogsOut(
                found.total,
                page,
                size,
                found.lines.map { (id, line) ->
                    WiredRoomLogsOut.List1Item(
                        id,
                        line.level.toByte(),
                        line.source.toByte(),
                        line.text,
                        line.atMillis,
                        shown.format(Instant.ofEpochMilli(line.atMillis)),
                    )
                },
                request.logLevelFilter >= 0,
                filtersEcho(request),
            )
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredRoomLogsOutCodec, event)
    }

    private fun filtersEcho(request: WiredGetRoomLogsIn): ByteArray {
        val echo = PacketWriter()
        if (request.logLevelFilter >= 0) echo.byte(request.logLevelFilter.toByte())
        echo.boolean(request.logSourceFilter >= 0)
        if (request.logSourceFilter >= 0) echo.byte(request.logSourceFilter.toByte())
        echo.boolean(request.query.isNotEmpty())
        if (request.query.isNotEmpty()) echo.string(request.query)
        return echo.toByteArray()
    }

    private fun settings(masks: WiredMasks) = WiredRoomSettingsOut(masks.modify, masks.read, masks.timezone)

    private companion object {
        const val OWNER = 4
        const val MAX_LOG_PAGE = 200
        const val BAD_PAGE: Short = 5
        const val MAX_TIMEZONE = 40
        val LOG_TIME: DateTimeFormatter
            get() = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(GameZone.current)
    }
}
