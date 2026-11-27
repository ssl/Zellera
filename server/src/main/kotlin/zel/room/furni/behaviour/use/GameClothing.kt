package zel.room.furni.behaviour.use

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import zel.game.catalog.Catalog
import zel.game.players.Player
import javax.sql.DataSource

class GameClothing(
    private val dataSource: DataSource,
    private val persist: (Runnable) -> Unit,
    private val catalog: () -> Catalog?,
) : FurniExchange.Clothing {
    override fun owns(
        player: Player,
        classname: String,
    ): Boolean? {
        val item = catalog()?.clothingSet(classname) ?: return null
        return player.inventory.wardrobe.clothing
            .contains(item.id)
    }

    override fun grant(
        player: Player,
        classname: String,
    ): Boolean {
        val item = catalog()?.clothingSet(classname) ?: return false
        val wardrobe = player.inventory.wardrobe
        if (wardrobe.clothing.contains(item.id)) return false
        wardrobe.addClothing(item.id, item.setId)
        return true
    }

    override fun settle(
        player: Player,
        classname: String,
    ) {
        val item = catalog()?.clothingSet(classname) ?: return
        val playerId = player.profile.id
        persist(Runnable { store(playerId, item.id) })
    }

    override fun release(
        player: Player,
        classname: String,
    ) {
        val item = catalog()?.clothingSet(classname) ?: return
        player.inventory.wardrobe.removeClothing(item.id, item.setId)
    }

    private fun store(
        playerId: Int,
        clothingId: Int,
    ) {
        log.orFallback("storing clothing $clothingId of player $playerId", Unit) {
            Sql(dataSource).write(INSERT) {
                int(playerId)
                int(clothingId)
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GameClothing::class.java)
        const val INSERT = "INSERT INTO player_clothing (player_id, clothing_id) VALUES (?, ?)"
    }
}
