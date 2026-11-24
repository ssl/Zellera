package zel.game.items

import zel.db.Params
import zel.db.write
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/** `limited_number` is the serial, `limited_total` the edition size. */
object OwnedFurniRows {
    /** Matches the row by id and by the owner the copy saw last or has now, so a traded-away furni is left alone. */
    const val OWNER_GUARD = " WHERE id = ? AND (player_id <=> ? OR player_id <=> ?)"

    const val PLACEMENT_SET =
        "UPDATE furni SET player_id = ?, room_id = ?, wall_position = ?, x = ?, y = ?, z = ?, direction = ?, " +
            "state = ?, limited_number = ?, limited_total = ?"

    private const val SAVE = PLACEMENT_SET + OWNER_GUARD

    private val log = LoggerFactory.getLogger(OwnedFurniRows::class.java)

    @Throws(SQLException::class)
    fun read(
        set: ResultSet,
        definition: Item,
    ): OwnedFurni =
        OwnedFurni(
            id = set.getInt("id"),
            userId = set.getInt("player_id"),
            definition = definition,
            state = set.getString("state").orEmpty(),
            editionSize = set.getInt("limited_total"),
            editionNumber = set.getInt("limited_number"),
            spot =
                FurniSpot(
                    set.getString("wall_position").orEmpty(),
                    set.getShort("x"),
                    set.getShort("y"),
                    set.getDouble("z"),
                    set.getInt("direction"),
                ),
            roomId = set.getInt("room_id"),
            guildId = set.getInt("guild_id"),
        )

    fun save(
        dataSource: DataSource,
        furni: OwnedFurni,
    ): Boolean {
        val owner = furni.ownerToSave()
        val saved =
            try {
                dataSource.connection.use { connection ->
                    connection.write(SAVE) {
                        placement(furni, owner)
                        ownerGuard(furni, owner)
                    } > 0
                }
            } catch (e: SQLException) {
                log.error("Furni {} not saved", furni.id, e)
                false
            }
        if (saved) furni.markSaved(owner) else furni.markChanged()
        return saved
    }
}

fun Params.placement(
    furni: OwnedFurni,
    owner: Int,
) {
    intOrNullWhenNotPositive(owner)
    intOrNullWhenNotPositive(furni.roomId)
    string(furni.spot.wallPosition)
    int(furni.spot.x.toInt())
    int(furni.spot.y.toInt())
    double(furni.spot.z)
    int(furni.spot.direction)
    string(furni.storedState)
    int(furni.editionNumber)
    int(furni.editionSize)
}

/** Binds the id, the owner last written and the owner being written. */
fun Params.ownerGuard(
    furni: OwnedFurni,
    owner: Int,
) {
    int(furni.id)
    intOrNullWhenNotPositive(furni.persistedUserId)
    intOrNullWhenNotPositive(owner)
}
