package zel.room.furni.behaviour.use

import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class JdbcPaperStore(
    private val dataSource: DataSource,
    private val persist: (Runnable) -> Unit,
) : RoomPaint.PaperStore {
    /** The paper only counts when its furni was still [ownerId]'s and is deleted in the same transaction. */
    override fun paint(
        roomId: Int,
        kind: String,
        value: String,
        rowId: Int,
        ownerId: Int,
    ) {
        val update = UPDATES[kind] ?: return
        persist(
            Runnable {
                log.orFallback("painting the $kind of room $roomId", Unit) {
                    dataSource.connection.use { c ->
                        c.allOrNothing {
                            val spent =
                                c.write(DELETE) {
                                    int(rowId)
                                    int(ownerId)
                                } == 1
                            if (spent) {
                                c.write(update) {
                                    string(value)
                                    int(roomId)
                                }
                            }
                            spent.takeIf { it }
                        }
                    }
                }
            },
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(JdbcPaperStore::class.java)
        const val DELETE = "DELETE FROM furni WHERE id = ? AND player_id = ?"
        val UPDATES =
            mapOf(
                "wallpaper" to "UPDATE rooms SET wall_paper = ? WHERE id = ?",
                "floor" to "UPDATE rooms SET floor_paper = ? WHERE id = ?",
                "landscape" to "UPDATE rooms SET landscape_paper = ? WHERE id = ?",
            )
    }
}
