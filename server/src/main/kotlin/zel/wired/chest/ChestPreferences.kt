package zel.wired.chest

import zel.protocol.generated.SetChestPreferencesIn

data class ChestPreferences(
    val name: String = "",
    val description: String = "",
    val everyoneOpens: Boolean = true,
    val everyoneDonates: Boolean = false,
    val lid: Int = LID_WHILE_VIEWED,
    val previewMode: Int = PREVIEW_UNSET,
    val previewAmount: Int = PREVIEW_MAX,
    val wiredEnabled: Boolean = false,
) {
    /**
     * The preview holds 1 to [PREVIEW_MAX] furni and the wired switch only goes on. The texts go to everyone in
     * the room and into owners' notices, so they pass [filter] and keep what their row holds.
     */
    fun after(
        m: SetChestPreferencesIn,
        filter: (String) -> String = { it },
    ): ChestPreferences =
        ChestPreferences(
            name = filter(oneLine(m.name).take(MAX_NAME)).take(MAX_NAME),
            description = filter(m.description.take(MAX_DESCRIPTION)).take(MAX_DESCRIPTION),
            everyoneOpens = m.accessOpen,
            everyoneDonates = m.accessDonate,
            lid = m.appearanceState,
            previewMode = m.previewMode,
            previewAmount = m.previewAmount.coerceIn(1, PREVIEW_MAX),
            wiredEnabled = wiredEnabled || m.enableWired,
        )

    fun setUp(
        starter: Boolean,
        starterTexts: () -> Pair<String, String>,
    ): ChestPreferences {
        val previewed = if (previewMode == PREVIEW_UNSET) copy(previewMode = PREVIEW_SET_UP) else this
        if (!starter || name.isNotEmpty() || description.isNotEmpty()) return previewed
        val (starterName, starterDescription) = starterTexts()
        return previewed.copy(name = starterName, description = starterDescription)
    }

    companion object {
        const val MAX_NAME = 100
        const val MAX_DESCRIPTION = 255

        private val BREAKS = Regex("\\s+")

        private fun oneLine(text: String): String = text.replace(BREAKS, " ").trim()

        const val LID_WHILE_VIEWED = 0
        const val LID_ALWAYS_OPEN = 1
        const val LID_CLOSED = 2
        const val PREVIEW_UNSET = -1
        const val PREVIEW_SET_UP = 2
        const val PREVIEW_MAX = 4
    }
}
