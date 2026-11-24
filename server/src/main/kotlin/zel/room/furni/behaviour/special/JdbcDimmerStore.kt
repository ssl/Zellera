package zel.room.furni.behaviour.special

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import zel.db.writeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class JdbcDimmerStore(
    private val dataSource: DataSource,
) : DimmerStore {
    override fun load(roomId: Int): Dimmers.Presets? =
        log.orFallback("reading the moodlight presets of room $roomId", null) {
            DimmerStore.presetsOf(
                Sql(dataSource).select(SELECT, roomId).rows { row ->
                    DimmerStore.StoredPreset(
                        row.getBoolean("selected"),
                        Dimmers.Preset(
                            row.getInt("preset"),
                            row.getBoolean("background_only"),
                            row.getString("color"),
                            row.getInt("brightness"),
                        ),
                    )
                },
            )
        }

    override fun save(
        roomId: Int,
        presets: Dimmers.Presets,
    ) {
        log.orFallback("saving the moodlight presets of room $roomId", Unit) {
            dataSource.connection.use { c ->
                c.allOrNothing {
                    c.write(DELETE) { int(roomId) }
                    c.writeEach(INSERT, presets.list.withIndex()) { (position, preset) ->
                        int(roomId)
                        int(position)
                        int(preset.id)
                        boolean(preset.id == presets.selected)
                        boolean(preset.backgroundOnly)
                        string(preset.colour)
                        int(preset.brightness)
                    }
                }
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(JdbcDimmerStore::class.java)
        const val SELECT =
            "SELECT preset, selected, background_only, color, brightness FROM room_dimmer_presets" +
                " WHERE room_id = ? ORDER BY position"
        const val DELETE = "DELETE FROM room_dimmer_presets WHERE room_id = ?"
        const val INSERT =
            "INSERT INTO room_dimmer_presets (room_id, position, preset, selected, background_only, color," +
                " brightness)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?)"
    }
}
