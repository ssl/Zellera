package zel.room.furni

import zel.core.config.DisplayNames

object BuildPass {
    const val FIRST_ID: Int = 0x7FFF0000
    const val OWNER_ID: Int = FIRST_ID

    /** Shown as the owner of build pass furni. */
    val ownerName: String get() = DisplayNames.buildPass

    fun isClientId(id: Int): Boolean = id >= FIRST_ID
}
