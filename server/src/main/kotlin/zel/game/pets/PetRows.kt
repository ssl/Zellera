package zel.game.pets

import zel.db.Params
import zel.db.insertForId
import zel.db.write
import java.sql.Connection
import java.sql.SQLException

object PetRows {
    const val SELECT =
        "SELECT pets.*, monster_plants.kind, monster_plants.color AS plant_color, monster_plants.nose, " +
            "monster_plants.nose_color, monster_plants.eyes, monster_plants.eyes_color, monster_plants.mouth, " +
            "monster_plants.mouth_color, monster_plants.base_palette, monster_plants.dies_at, " +
            "monster_plants.is_dead, monster_plants.shown_stage, monster_plants.breedable, " +
            "monster_plants.allow_breeding, horses.mane_style, horses.mane_tint, horses.saddle_definition_id, " +
            "horses.open_to_riders, pet_looks.parts AS look_parts " +
            "FROM pets LEFT JOIN monster_plants ON monster_plants.pet_id = pets.id " +
            "LEFT JOIN horses ON horses.pet_id = pets.id LEFT JOIN pet_looks ON pet_looks.pet_id = pets.id"

    private const val TO_INVENTORY =
        "UPDATE pets SET room_id = NULL, born_at = ?, experience = ?, energy = ?, happiness = ?, thirst = ?, " +
            "respect = ? WHERE id = ?"
    private const val INSERT =
        "INSERT INTO pets (player_id, type_id, race, color, name, born_at, experience, energy, happiness, thirst, " +
            "respect) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    private const val INSERT_PLANT = "INSERT INTO monster_plants (pet_id) VALUES (?)"

    private const val SAVE_LOOK =
        "INSERT INTO pet_looks (pet_id, parts) VALUES (?, ?) ON DUPLICATE KEY UPDATE parts = VALUES(parts)"

    private const val SAVE_HORSE =
        "INSERT INTO horses (pet_id, mane_style, mane_tint, saddle_definition_id, open_to_riders) " +
            "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE mane_style = VALUES(mane_style), " +
            "mane_tint = VALUES(mane_tint), saddle_definition_id = VALUES(saddle_definition_id), " +
            "open_to_riders = VALUES(open_to_riders)"

    /** What only a horse stores; [saddleDefinitionId] is 0 without a saddle. */
    data class HorseRow(
        val petId: Int,
        val maneStyle: Int,
        val maneTint: Int,
        val saddleDefinitionId: Int,
        val openToRiders: Boolean,
    )

    fun update(
        connection: Connection,
        row: PetRow,
    ) {
        val changed =
            connection.write(TO_INVENTORY) {
                int(row.bornAt)
                condition(row.stats)
                int(row.id)
            }
        if (changed != 1) throw SQLException("Unable to update pet ${row.id}")
    }

    fun insert(
        connection: Connection,
        row: PetRow,
    ): Int {
        val id =
            connection.insertForId(INSERT) {
                int(row.ownerId)
                int(row.type)
                int(row.race)
                string(row.color)
                string(row.name)
                int(row.bornAt)
                condition(row.stats)
            } ?: throw SQLException("pet insert returned no id")
        if (row.type == PetKind.MONSTERPLANT_TYPE) connection.write(INSERT_PLANT) { int(id) }
        return id
    }

    private fun Params.condition(stats: PetStats) {
        int(stats.experience)
        int(stats.energy)
        int(stats.happiness)
        int(stats.thirst)
        int(stats.respect)
    }

    fun saveLook(
        connection: Connection,
        id: Int,
        parts: String,
    ) {
        connection.write(SAVE_LOOK) {
            int(id)
            string(parts)
        }
    }

    fun saveHorse(
        connection: Connection,
        horse: HorseRow,
    ) {
        connection.write(SAVE_HORSE) {
            int(horse.petId)
            int(horse.maneStyle)
            int(horse.maneTint)
            intOrNullWhenNotPositive(horse.saddleDefinitionId)
            boolean(horse.openToRiders)
        }
    }
}
