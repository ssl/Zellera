package zel.ext

import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.OutgoingCodec
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.entry.ArrivalSpots
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import org.slf4j.Logger
import java.nio.file.Path

interface ExtensionHost {
    val name: String

    val folder: Path

    val log: Logger

    val rooms: ExtensionRooms

    val values: PlayerValues

    /** Settings row `extensions.<name>.<key>`, else the default in `extension.json`. */
    fun setting(key: String): String?

    fun onEvent(listener: RoomListener)

    fun onTick(part: TickPart)

    fun onRoom(part: RoomPart)

    /** True takes the click: no walk starts. */
    fun onClick(claim: ClickClaim)

    fun onArrival(spots: ArrivalSpots)

    /** Return the message, another one, or null to take it from the server. */
    fun <T> onPacketIn(
        codec: IncomingCodec<T>,
        hook: PacketHook<T>,
    )

    /** Once per receiving player; null drops it. */
    fun <T> onPacketOut(
        codec: OutgoingCodec<T>,
        hook: PacketHook<T>,
    )

    fun onServerEvent(listener: (ServerEvent) -> Unit)

    fun <T> send(
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ): Boolean
}

fun interface ClickClaim {
    fun takes(
        scope: RoomScope,
        unit: RoomUnit,
        clicked: Point,
    ): Boolean
}
