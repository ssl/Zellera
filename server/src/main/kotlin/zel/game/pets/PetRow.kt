package zel.game.pets

import java.sql.ResultSet

data class PetRow(
    val id: Int,
    val ownerId: Int,
    val name: String?,
    val type: Int,
    val race: Int,
    val color: String?,
    val bornAt: Int,
    val stats: PetStats,
) {
    companion object {
        fun read(set: ResultSet): PetRow =
            PetRow(
                id = set.getInt("id"),
                ownerId = set.getInt("player_id"),
                name = set.getString("name"),
                type = set.getInt("type_id"),
                race = set.getInt("race"),
                color = set.getString("color"),
                bornAt = set.getInt("born_at"),
                stats = PetStats.stored(set),
            )
    }
}
