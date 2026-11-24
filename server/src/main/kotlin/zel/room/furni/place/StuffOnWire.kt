package zel.room.furni.place

import zel.protocol.generated.StuffData
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni

interface StuffOnWire : FurniBehaviour {
    fun stuff(furni: RoomFurni): StuffData
}
