package zel.wired.variable.addon

import com.google.gson.JsonObject
import zel.room.Room
import zel.room.furni.RoomFurni

interface ImportRows {
    fun extras(
        room: Room,
        box: RoomFurni,
    ): JsonObject?

    fun updateExtras(
        room: Room,
        box: RoomFurni,
        change: (JsonObject) -> Unit,
    )
}
