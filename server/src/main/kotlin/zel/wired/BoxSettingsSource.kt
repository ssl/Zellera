package zel.wired

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.wired.box.BoxSettings

/** Null for a box never saved, which runs with its fresh form. */
fun interface BoxSettingsSource {
    fun settings(
        room: Room,
        box: RoomFurni,
    ): BoxSettings?
}
