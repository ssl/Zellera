package zel.wired.box

data class BoxForm(
    val dialogCode: Int,
    val fresh: BoxSettings,
    val furniSlots: List<SourceSlot>,
    val userSlots: List<SourceSlot>,
    val allowWallItems: Boolean,
    /** Null for the room's selection limit. */
    val furniLimit: Int? = null,
    /** Longest text the box keeps, in characters. */
    val textLimit: Int = TEXT_LIMIT,
) {
    companion object {
        const val FURNI_LIMIT = 20
        const val TEXT_LIMIT = 1024
    }
}
