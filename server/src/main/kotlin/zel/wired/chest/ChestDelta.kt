package zel.wired.chest

import zel.room.furni.RoomFurni

class ChestDelta(
    val furni: RoomFurni,
    val chest: Chest,
    val transactionId: Long,
) {
    val deposited = ArrayList<StoredFurni>()
    val withdrawn = ArrayList<StoredFurni>()
    var creditsIn = 0
    var creditsOut = 0
}
