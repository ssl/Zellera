package zel.wired.addon.label

data class Label(
    val text: String = "",
    val color: Int = DEFAULT_COLOR,
    val visibility: Int = WIRED,
    val alwaysOpen: Boolean = false,
) {
    fun visibleTo(mayReadWired: Boolean): Boolean = visibility == EVERYONE || mayReadWired

    /** Only a label everyone sees can stay open. */
    fun withSettings(
        color: Int,
        visibility: Int,
        alwaysOpen: Boolean,
    ): Label = copy(color = color, visibility = visibility, alwaysOpen = visibility == EVERYONE && alwaysOpen)

    companion object {
        const val CODE = "wf_label"
        const val EVERYONE = 0
        const val WIRED = 1
        const val DEFAULT_COLOR = 4278912
        const val MAX_TEXT_BYTES = 1000
        const val TOO_MUCH_TEXT = "wiredfurni.error.too_much_text"
        const val INVALID_SETTINGS = "wiredfurni.error.label.invalid_settings"
        const val NO_WRITE_RIGHTS = "wiredfurni.error.no_write_rights"
        private const val LAST_BMP_CODE_POINT = 0xFFFF

        val COLORS: List<Int> = listOf(4278912, 2651759, 4157508, 8416562, 8409402, 8404554, 6834048, 5592405)

        fun validSettings(
            color: Int,
            visibility: Int,
        ): Boolean = color in COLORS && (visibility == EVERYONE || visibility == WIRED)

        fun tooLong(text: String): Boolean = text.toByteArray(Charsets.UTF_8).size > MAX_TEXT_BYTES

        fun ignored(text: String): Boolean = text.codePoints().anyMatch { it > LAST_BMP_CODE_POINT }
    }
}
