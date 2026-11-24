package zel.game.pets

import java.sql.ResultSet

/** A trick a pet can learn (`pet_commands`); identity matters, as a type's own copy is a different trick. */
class PetCommand(
    val id: Int,
    val word: String?,
    val level: Int,
    val xp: Int,
) {
    fun openingAt(level: Int): PetCommand = PetCommand(id, word, level, xp)

    companion object {
        fun stored(row: ResultSet): PetCommand =
            PetCommand(
                id = row.getInt("id"),
                word = row.getString("word"),
                level = row.getInt("level"),
                xp = row.getInt("experience"),
            )
    }
}
