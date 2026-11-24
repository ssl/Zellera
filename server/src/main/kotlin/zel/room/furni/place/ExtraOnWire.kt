package zel.room.furni.place

import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni

interface ExtraOnWire : FurniBehaviour {
    fun extra(furni: RoomFurni): Int
}
