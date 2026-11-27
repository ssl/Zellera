package zel.room.rights

import zel.core.rules.rules
import zel.room.entry.DoorSecret

object SettingsCheck {
    const val ERROR_NAME = 7
    const val ERROR_PASSWORD = 5
    const val ERROR_STAFF_TAG = 12
    const val ERROR_TAG = 13
    const val NAME_LENGTH = 50
    const val DESCRIPTION_LENGTH = 255
    const val TAG_LENGTH = 15
    const val MAX_TAGS = 2
    const val PASSWORD_DOOR = 2
    private val DOORS = 0..3
    private val USERS = 1..200

    private const val MIN_USERS = 10
    private val TRADE = 0..2
    private val THICKNESS = -2..1
    private val CHOICES = 0..2

    fun check(
        wanted: RoomSettings,
        current: RoomSettings,
        staffTags: Set<String>,
        categoryAllowed: Boolean,
        filter: (String) -> String = { it },
    ): SettingsAnswer {
        val cleaned =
            wanted.copy(
                name = filter(wanted.name),
                description = filter(wanted.description).take(DESCRIPTION_LENGTH),
                tags = wanted.tags.map { filter(it).take(TAG_LENGTH) },
            )
        if (wanted.description.length > DESCRIPTION_LENGTH) return SettingsAnswer.Ignored
        if (!inRanges(cleaned) || !categoryAllowed) return SettingsAnswer.Ignored
        if (cleaned.name.isBlank()) return SettingsAnswer.Refused(ERROR_NAME, "")
        if (cleaned.doorMode == PASSWORD_DOOR && cleaned.password.length !in 1..DoorSecret.MAX_LENGTH) {
            return SettingsAnswer.Refused(ERROR_PASSWORD, "")
        }
        if (wanted.tags.any { it.length > TAG_LENGTH || ';' in it }) return SettingsAnswer.Refused(ERROR_TAG, "")
        val tags = cleaned.tags.distinct().filter { it.isNotEmpty() }
        if (tags.any { it in staffTags }) return SettingsAnswer.Refused(ERROR_STAFF_TAG, STAFF_TAG_TEXT)
        return SettingsAnswer.Saved(
            cleaned.copy(
                ownerId = current.ownerId,
                name = cleaned.name.take(NAME_LENGTH),
                maxUsers = cleaned.maxUsers.coerceAtLeast(MIN_USERS),
                password = if (cleaned.doorMode == PASSWORD_DOOR) cleaned.password else current.password,
                tags = tags,
                idleSleepSeconds = if (cleaned.idleSleep) cleaned.idleSleepSeconds else 0,
                idleKickSeconds = if (cleaned.idleKick) cleaned.idleKickSeconds else 0,
            ),
        )
    }

    private fun inRanges(s: RoomSettings): Boolean =
        s.doorMode in DOORS &&
            s.maxUsers in USERS &&
            s.tradeMode in TRADE &&
            s.wallThickness in THICKNESS &&
            s.floorThickness in THICKNESS &&
            s.floodFilter in CHOICES &&
            s.whoCanMute in CHOICES &&
            s.whoCanKick in CHOICES &&
            s.whoCanBan in CHOICES &&
            s.description.length <= DESCRIPTION_LENGTH &&
            s.tags.size <= MAX_TAGS &&
            (!s.idleSleep || s.idleSleepSeconds in rules.roomSleepSeconds) &&
            (!s.idleKick || s.idleKickSeconds in rules.roomKickSeconds)

    private const val STAFF_TAG_TEXT = "1"
}
