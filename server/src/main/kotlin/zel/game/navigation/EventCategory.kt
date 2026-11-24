package zel.game.navigation

import zel.core.Configuration

/** A room event category from the setting `navigator.event_categories` ("id,caption,visible;..."). */
data class EventCategory(
    val id: Int,
    val caption: String,
    val isVisible: Boolean,
) {
    companion object {
        private const val SETTING = "navigator.event_categories"
        private const val FIELDS = 3

        fun parse(text: String): EventCategory? {
            val fields = text.split(",")
            if (fields.size != FIELDS) return null
            val id = fields[0].trim().toIntOrNull() ?: return null
            return EventCategory(id, fields[1], fields[2].trim().toBoolean())
        }

        fun configured(config: Configuration): List<EventCategory> =
            config
                .getValue(SETTING, "")
                .split(";")
                .filter { it.isNotBlank() }
                .mapNotNull(::parse)
    }
}
