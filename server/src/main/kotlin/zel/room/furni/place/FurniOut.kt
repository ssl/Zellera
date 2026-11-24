package zel.room.furni.place

import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane

object FurniOut {
    private const val DIALOG = "furni_placement_error"
    private const val MESSAGE = "message"

    fun dialogOf(message: String): NotificationDialogOut =
        NotificationDialogOut(DIALOG, listOf(NotificationDialogOut.List1Item(MESSAGE, message)))

    fun error(
        scope: RoomScope,
        playerId: Int,
        error: PlaceError,
    ) = dialog(scope, playerId, error.message)

    fun dialog(
        scope: RoomScope,
        playerId: Int,
        message: String,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), NotificationDialogOutCodec, dialogOf(message))

    fun snapBack(
        scope: RoomScope,
        playerId: Int,
        error: PlaceError,
        furni: RoomFurni,
    ) {
        error(scope, playerId, error)
        val back = FurniWire.objectUpdate(furni)
        scope.out.send(Lane.Now, Audience.Player(playerId), ObjectUpdateOutCodec, back)
    }

    fun heights(
        scope: RoomScope,
        change: HeightMapChange,
    ) = change.updates().forEach { scope.out.send(Lane.Now, Audience.Room, HeightMapUpdateOutCodec, it) }
}
