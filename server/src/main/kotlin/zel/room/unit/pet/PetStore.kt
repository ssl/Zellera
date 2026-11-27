package zel.room.unit.pet

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import zel.game.pets.Pet
import zel.game.pets.PetRows
import zel.room.map.Point
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class PetStore(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    data class Stored(
        val pet: Pet,
        val tile: Point,
        val direction: Int,
    )

    /** What a save writes, copied under the room lock. */
    data class Row(
        val id: Int,
        val roomId: Int,
        val experience: Int,
        val energy: Int,
        val respect: Int,
        val x: Int,
        val y: Int,
        val z: Double,
        val direction: Int,
        val thirst: Int,
        val happiness: Int,
    )

    data class PlantClock(
        val id: Int,
        val created: Int,
        val deathAt: Int,
    )

    data class PlantBreedFlags(
        val id: Int,
        val breedable: Boolean,
        val allowsBreeding: Boolean,
    )

    data class HorseLook(
        val id: Int,
        val palette: Int,
        val hairStyle: Int,
        val hairColor: Int,
        val saddle: Boolean,
        val anyoneRides: Boolean,
        val saddleDefinitionId: Int,
    )

    data class RoomSwitches(
        val allowOtherPets: Boolean,
        val muteAllPets: Boolean,
    )

    fun loadRoom(
        roomId: Int,
        build: (ResultSet) -> Pet?,
    ): List<Stored> =
        log.orFallback("loading the pets of room $roomId", emptyList()) {
            sql
                .select(ROOM_SQL, roomId)
                .rows { set ->
                    build(set)?.let { Stored(it, Point(set.getInt("x"), set.getInt("y")), set.getInt("direction")) }
                }.filterNotNull()
        }

    fun switches(roomId: Int): RoomSwitches =
        log.orFallback("reading the pet switches of room $roomId", NO_SWITCHES) {
            sql.select(SWITCHES_SQL, roomId).first { RoomSwitches(it.getBoolean(1), it.getBoolean(2)) } ?: NO_SWITCHES
        }

    fun save(row: Row) {
        log.orFallback("saving pet ${row.id}", Unit) {
            sql.write(UPDATE_SQL) { bindRow(row) }
        }
    }

    fun saveAll(rows: Collection<Row>) {
        if (rows.isEmpty()) return
        log.orFallback("saving ${rows.size} pets", Unit) {
            sql.writeEach(UPDATE_SQL, rows) { bindRow(it) }
        }
    }

    fun savePlant(clock: PlantClock) {
        log.orFallback("saving plant ${clock.id}", Unit) {
            sql.write(PLANT_SQL) {
                int(clock.created)
                intOrNull(clock.deathAt)
                int(clock.id)
            }
        }
    }

    fun savePlantBreeding(flags: PlantBreedFlags) {
        log.orFallback("saving the breeding flags of plant ${flags.id}", Unit) {
            sql.write(PLANT_BREED_SQL, flags.breedable, flags.allowsBreeding, flags.id)
        }
    }

    /** Saves the look and runs [alongside] in one transaction; null when nothing was saved. */
    fun <T : Any> saveHorse(
        look: HorseLook,
        alongside: (Connection) -> T?,
    ): T? =
        log.orFallback("saving horse ${look.id}", null) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    writeHorse(connection, look)
                    alongside(connection)
                }
            }
        }

    fun saveHorse(look: HorseLook) {
        log.orFallback("saving horse ${look.id}", Unit) {
            dataSource.connection.use { connection -> connection.allOrNothing { writeHorse(connection, look) } }
        }
    }

    /** The coat is the pet's palette; mane, saddle and riders are the horse's own row. */
    private fun writeHorse(
        connection: Connection,
        look: HorseLook,
    ) {
        connection.write(COAT_SQL) {
            int(look.palette)
            int(look.id)
        }
        val saddle = if (look.saddle) look.saddleDefinitionId else 0
        val row = PetRows.HorseRow(look.id, look.hairStyle, look.hairColor, saddle, look.anyoneRides)
        PetRows.saveHorse(connection, row)
    }

    /** False when it was not theirs. */
    fun deleteFurni(
        rowId: Int,
        ownerId: Int,
    ): Boolean =
        log.orFallback("deleting spent furni $rowId", false) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val deleted =
                        connection.write(DELETE_FURNI_SQL) {
                            int(rowId)
                            intOrNull(ownerId)
                        } == 1
                    if (deleted) connection.write(DELETE_BUILD_PASS_SQL) { int(rowId) }
                    deleted.takeIf { it }
                } ?: false
            }
        }

    data class NestSpend(
        val deleted: Boolean,
        val baby: Pet?,
    )

    fun spendNest(
        rowId: Int,
        ownerId: Int,
        saveBaby: (Connection) -> Pet?,
    ): NestSpend =
        log.orFallback("spending nest furni $rowId", NestSpend(false, null)) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val deleted =
                        connection.write(DELETE_FURNI_SQL) {
                            int(rowId)
                            intOrNull(ownerId)
                        } == 1
                    if (!deleted) {
                        null
                    } else {
                        connection.write(DELETE_BUILD_PASS_SQL) { int(rowId) }
                        NestSpend(true, saveBaby(connection))
                    }
                } ?: NestSpend(false, null)
            }
        }

    fun markDead(id: Int): Boolean =
        log.orFallback("saving the death of plant $id", false) { sql.write(DEAD_SQL, id) > 0 }

    fun delete(id: Int): Boolean = log.orFallback("deleting pet $id", false) { sql.write(DELETE_SQL, id) == 1 }

    private fun Params.bindRow(row: Row) {
        intOrNull(row.roomId)
        int(row.x)
        int(row.y)
        double(row.z)
        int(row.direction)
        int(row.experience)
        int(row.energy)
        int(row.happiness)
        int(row.thirst)
        int(row.respect)
        int(row.id)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PetStore::class.java)
        val NO_SWITCHES = RoomSwitches(allowOtherPets = false, muteAllPets = false)

        const val ROOM_SQL = PetRows.SELECT + " WHERE pets.room_id = ? ORDER BY pets.id"
        const val SWITCHES_SQL = "SELECT pets_allowed, pets_muted FROM rooms WHERE id = ? LIMIT 1"
        const val UPDATE_SQL =
            "UPDATE pets SET room_id = ?, x = ?, y = ?, z = ?, direction = ?, experience = ?, energy = ?, " +
                "happiness = ?, thirst = ?, respect = ? WHERE id = ?"
        const val PLANT_SQL =
            "UPDATE pets JOIN monster_plants ON monster_plants.pet_id = pets.id " +
                "SET pets.born_at = ?, monster_plants.dies_at = ?, monster_plants.is_dead = FALSE WHERE pets.id = ?"
        const val DEAD_SQL = "UPDATE monster_plants SET is_dead = TRUE WHERE pet_id = ? AND is_dead = FALSE"
        const val PLANT_BREED_SQL = "UPDATE monster_plants SET breedable = ?, allow_breeding = ? WHERE pet_id = ?"
        const val COAT_SQL = "UPDATE pets SET race = ? WHERE id = ?"
        const val DELETE_FURNI_SQL = "DELETE FROM furni WHERE id = ? AND player_id <=> ? LIMIT 1"
        const val DELETE_SQL = "DELETE FROM pets WHERE id = ? LIMIT 1"
        const val DELETE_BUILD_PASS_SQL = "DELETE FROM build_pass_furni WHERE furni_id = ?"
    }
}
