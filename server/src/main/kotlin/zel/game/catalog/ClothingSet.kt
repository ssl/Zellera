package zel.game.catalog

import java.sql.ResultSet
import java.sql.SQLException

class ClothingSet(
    val id: Int,
    val name: String?,
    val setId: IntArray,
) {
    companion object {
        @Throws(SQLException::class)
        fun fromRow(row: ResultSet): ClothingSet =
            ClothingSet(
                row.getInt("id"),
                row.getString("name"),
                row
                    .getString("set_ids")
                    .orEmpty()
                    .split(',')
                    .filter(String::isNotBlank)
                    .map { it.trim().toInt() }
                    .toIntArray(),
            )
    }
}
