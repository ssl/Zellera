package zel.room.furni.behaviour.special

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.first
import zel.db.orFallback
import zel.db.write
import zel.game.items.CrackableKind
import zel.game.items.Item
import zel.game.items.ItemServices
import zel.game.items.mysterybox.MysteryBoxPayout
import zel.game.permissions.Privilege
import zel.game.pets.PetPackage
import zel.net.clients.ClientConnections
import zel.protocol.events.MysteryBoxMessages
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class GameSpecialHost(
    private val dataSource: DataSource,
    private val clients: ClientConnections,
    private val items: () -> ItemServices?,
    private val checksColour: () -> Boolean = { true },
    private val furniLeft: (Int) -> Unit = {},
) : SpecialHost {
    override fun checksDimmerColour(): Boolean = checksColour()

    override fun mayEditAds(playerId: Int): Boolean {
        val player = clients.onlinePlayer(playerId) ?: return false
        if (player.hasPermission(Privilege.ROOM_ADVERTISEMENTS)) return true
        player.alert(player.services.texts.text(ADS_REFUSED_TEXT))
        return false
    }

    private val mysteryMessages = MysteryBoxMessages()

    override fun deleteSpent(
        roomId: Int,
        rowId: Int,
    ): Boolean {
        val owner =
            log.orFallback("deleting spent furni $rowId", null) {
                dataSource.connection.use { connection ->
                    connection.allOrNothing {
                        val owner = connection.first(OWNER_OF, { int(rowId) }) { it.getInt(1) }
                        val deleted =
                            connection.write(DELETE_SPENT) {
                                int(rowId)
                                int(roomId)
                            } == 1
                        if (deleted) connection.write(DELETE_BUILD_PASS_ROW) { int(rowId) }
                        owner?.takeIf { deleted }
                    }
                }
            } ?: return false
        furniLeft(owner)
        return true
    }

    override fun mysteryPrize(
        playerId: Int,
        colour: String,
    ) {
        val services = items() ?: return
        val player = clients.onlinePlayer(playerId) ?: return
        val classId = MysteryBoxPayout(services).grant(player, colour)
        player.client?.let { mysteryMessages.prize(it, PRIZE_TYPE_FURNI, classId) }
    }

    override fun crackable(definitionId: Int): CrackableKind? = items()?.items()?.sideTables?.crackable(definitionId)

    override fun playerName(playerId: Int): String =
        clients
            .onlinePlayer(playerId)
            ?.profile
            ?.name
            .orEmpty()

    override fun crackPrize(definitionId: Int): Item? = items()?.items()?.drawCrackablePrize(definitionId)

    override fun createFurni(
        ownerId: Int,
        definition: Item,
        state: String,
    ): Int =
        items()
            ?.items()
            ?.newFurni(ownerId, definition, 0, 0, state)
            ?.id ?: 0

    override fun putInRoom(
        furniId: Int,
        roomId: Int,
        x: Int,
        y: Int,
        z: Double,
        direction: Int,
    ) {
        val sql = "UPDATE furni SET room_id = ?, wall_position = '', x = ?, y = ?, z = ?, direction = ? WHERE id = ?"
        update(sql, roomId, x, y, z, direction, furniId)
    }

    override fun giveFurni(
        playerId: Int,
        definition: Item,
    ) {
        val item = items()?.items()?.newFurni(playerId, definition, 0, 0, "0") ?: return
        val player = clients.onlinePlayer(playerId) ?: return
        player.inventory.furni.put(item)
        player.client?.let {
            it.send(AddRoomItem(item))
            it.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        }
    }

    override fun membership(
        playerId: Int,
        type: String,
        seconds: Int,
    ) {
        clients
            .onlinePlayer(playerId)
            ?.stats
            ?.club
            ?.create(type, seconds)
    }

    override fun wearEffect(
        playerId: Int,
        effect: Int,
    ) {
        val effects = clients.onlinePlayer(playerId)?.inventory?.effects ?: return
        if (!effects.owns(effect)) effects.grant(effect)
        effects.wear(effect)
    }

    override fun petPackage(itemName: String): PetPackage? = items()?.pets()?.catalog?.packageNamed(itemName)

    private fun update(
        sql: String,
        vararg values: Any?,
    ): Int = log.orFallback("furni write $sql", 0) { Sql(dataSource).write(sql, *values) }

    private companion object {
        const val OWNER_OF = "SELECT player_id FROM furni WHERE id = ?"
        const val DELETE_SPENT =
            "DELETE FROM furni WHERE id = ? AND room_id = ?" +
                " AND NOT EXISTS (SELECT 1 FROM wired_spawned_furni WHERE furni_id = furni.id)"
        const val DELETE_BUILD_PASS_ROW = "DELETE FROM build_pass_furni WHERE furni_id = ?"
        const val PRIZE_TYPE_FURNI = "furni"
        const val ADS_REFUSED_TEXT = "room.furni.ad.not_allowed"
        val log: Logger = LoggerFactory.getLogger(GameSpecialHost::class.java)
    }
}
