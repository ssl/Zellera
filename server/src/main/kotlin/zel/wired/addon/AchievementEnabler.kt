package zel.wired.addon

import zel.room.Room
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource
import zel.wired.box.AddonBox
import zel.wired.box.BoxSettings

class AchievementEnabler(
    override val furniId: Int,
    override val settings: BoxSettings,
) : AddonBox {
    companion object {
        const val CODE = "wf_xtra_achievement_enabler"
        private const val PREFIX = "ACH_WF_"

        /** What is left of an achievement code's 64 characters after the prefix. */
        private const val MAX_NAME = 57

        private val SEPARATORS = Regex("[,;\\s]+")

        /** Names a line each, as the dialog asks for them, or parted by commas, semicolons or blanks. */
        fun parse(text: String): List<String> =
            text
                .splitToSequence(SEPARATORS)
                .map { it.removePrefix(PREFIX) }
                .filter { it.length in 1..MAX_NAME && it.all(::isNameChar) }
                .distinct()
                .toList()

        /** The text as the box keeps it: the names that count, one a line. */
        fun kept(text: String): String = parse(text).joinToString("\n")

        private fun isNameChar(char: Char): Boolean =
            char == '_' || char == '-' || char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'

        fun namesIn(
            room: Room,
            forms: BoxSettingsSource,
        ): List<String> =
            room.furni
                .all()
                .filter { Interactions.of(it.definition) == CODE }
                .sortedBy { it.id }
                .flatMap { parse(forms.settings(room, it)?.text.orEmpty()) }
                .distinct()
    }
}
