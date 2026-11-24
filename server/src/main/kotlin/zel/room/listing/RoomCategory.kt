package zel.room.listing

class RoomCategory(
    val id: Int,
    val minRank: Int,
    val captionKey: String,
    val caption: String,
    name: String,
    val isVisible: Boolean,
    val isAutomatic: Boolean,
    automaticCategoryKey: String,
    val isStaffOnly: Boolean,
    val maxUserCount: Int,
    val order: Int,
    /** Rooms in a public category never unload while empty. */
    val isPublic: Boolean = false,
) : Comparable<RoomCategory> {
    val name: String = name.ifEmpty { caption }

    val automaticCategoryKey: String = automaticCategoryKey.ifEmpty { "NONE" }

    val globalCategoryKey: String = globalKeyOf(caption)

    override fun compareTo(other: RoomCategory): Int = compareValuesBy(this, other, { it.order }, { it.id })

    companion object {
        const val PERSONAL_KEY = "PERSONAL"
        const val GLOBAL_PREFIX = "\${navigator.flatcategory.global."

        fun globalKeyOf(caption: String?): String {
            val text = caption?.trim().orEmpty()
            return if (text.startsWith(GLOBAL_PREFIX) && text.endsWith("}")) {
                text.substring(GLOBAL_PREFIX.length, text.length - 1)
            } else {
                ""
            }
        }
    }
}
