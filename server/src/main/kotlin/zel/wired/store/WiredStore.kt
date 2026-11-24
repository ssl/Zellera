package zel.wired.store

import zel.wired.form.WiredMasks

interface WiredStore {
    fun roomBoxes(roomId: Int): Map<Int, StoredBox>

    fun saveBox(
        furniId: Int,
        box: StoredBox,
    )

    fun masks(roomId: Int): WiredMasks?

    fun saveMasks(
        roomId: Int,
        masks: WiredMasks,
    )
}
