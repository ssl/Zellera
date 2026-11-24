package zel.game.moderation

import java.sql.ResultSet

/** Lengths are hours, 0 for none. */
data class SanctionPreset(
    val id: Int,
    val name: String,
    val message: String,
    val banHours: Int,
    val muteHours: Int,
) {
    companion object {
        fun fromRow(row: ResultSet) =
            SanctionPreset(
                id = row.getInt("id"),
                name = row.getString("name").orEmpty(),
                message = row.getString("message").orEmpty(),
                banHours = row.getInt("ban_hours"),
                muteHours = row.getInt("mute_hours"),
            )
    }
}
