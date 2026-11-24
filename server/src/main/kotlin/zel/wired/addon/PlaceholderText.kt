package zel.wired.addon

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.wired.BoxSettingsSource
import zel.wired.box.BoxSettings

class PlaceholderText(
    settings: BoxSettings,
) {
    private val parts = settings.text.split('\t')
    private val multiple = settings.intAt(0) == 1

    val name: String = unwrap(parts.first().trim())

    private val delimiter: String =
        parts.getOrNull(1)?.take(MAX_DELIMITER)?.takeIf { multiple && it.isNotEmpty() } ?: DEFAULT_DELIMITER

    /** Lazy, so a long list of targets is read only as far as [Placeholders.MAX_TEXT]. */
    fun join(values: Sequence<String>): String? =
        if (multiple) Placeholders.join(values, delimiter) else values.firstOrNull()?.let(Placeholders::cap)

    companion object {
        const val MAX_NAME = 40
        private val OUTPUT_BOXES =
            setOf("wf_xtra_text_output_username", "wf_xtra_text_output_furni_name", "wf_xtra_text_output_variable")

        fun namesOn(
            room: Room,
            tile: Point,
            forms: BoxSettingsSource,
        ): List<String> =
            room.furni
                .all()
                .filter { it.anchor == tile && Interactions.of(it.definition) in OUTPUT_BOXES }
                .sortedWith(compareBy<RoomFurni> { it.z }.thenBy { it.id })
                .mapNotNull { box -> forms.settings(room, box)?.let { PlaceholderText(it).name } }
                .filter { it.isNotEmpty() }

        const val MAX_DELIMITER = 16
        private const val DEFAULT_DELIMITER = ", "

        fun unwrap(name: String): String =
            if (name.startsWith("$(") && name.endsWith(")")) name.substring(2, name.length - 1) else name
    }
}
