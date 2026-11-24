package zel.game.items

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.insertForId
import zel.db.write
import zel.game.catalog.Catalog
import zel.game.catalog.RecyclerPrizes
import zel.game.players.Player
import zel.util.GameZone
import java.sql.Connection
import java.sql.SQLException

class PrizeBoxes internal constructor(
    private val queries: ItemQueries,
    private val catalog: () -> Catalog?,
    private val loadFurni: (Connection, Int, Int?) -> OwnedFurni?,
) {
    @Throws(SQLException::class)
    fun recycle(
        connection: Connection,
        player: Player,
        prize: RecyclerPrizes.Entry,
    ): OwnedFurni? {
        val boxDefinition = catalog()?.recycler?.boxItem
        if (boxDefinition == null) {
            LOGGER.warn("No recycler box furni: nothing to put the prize in")
            return null
        }
        val furniId =
            connection.insertForId(NEW_BOX_SQL) {
                int(player.profile.id)
                int(boxDefinition.id)
                string(BoxOpenings.boxDay(GameZone.today()))
            } ?: return null
        connection.write(NEW_BOX_PRIZE_SQL) {
            int(furniId)
            intOrNull(prize.item?.id ?: 0)
            intOrNull(prize.chatStyleId)
        }
        return loadFurni(connection, furniId, null)
    }

    /** Null when the box gives nothing. */
    fun openRecyclerBox(
        player: Player,
        boxId: Int,
        now: Int,
    ): BoxPrize? =
        queries.withConnection { connection ->
            when (val prize = BoxOpenings.openRecyclerBox(connection, boxId, player.profile.id, now)) {
                is BoxOpenings.Prize.Furni -> {
                    loadFurni(
                        connection,
                        prize.furniId,
                        player.profile.id,
                    )?.let(BoxPrize::Furni)
                }

                is BoxOpenings.Prize.ChatStyle -> {
                    BoxPrize.ChatStyle(prize.styleId)
                }

                null -> {
                    null
                }
            }
        }

    sealed interface BoxPrize {
        data class Furni(
            val furni: OwnedFurni,
        ) : BoxPrize

        data class ChatStyle(
            val styleId: Int,
        ) : BoxPrize
    }

    /** Null if the gift does not open; unreadable contents show at the next login. */
    fun openGift(
        player: Player,
        giftId: Int,
        contents: IntArray?,
    ): List<OwnedFurni>? {
        val openerId = player.profile.id
        val contentIds = BoxOpenings.giftContents(contents)
        return queries.withConnection { connection ->
            if (BoxOpenings.openGift(connection, giftId, openerId, contentIds)) {
                contentIds.toList().mapNotNull { readBack(connection, it, openerId) }
            } else {
                null
            }
        }
    }

    private fun readBack(
        connection: Connection,
        furniId: Int,
        ownerId: Int,
    ): OwnedFurni? =
        try {
            loadFurni(connection, furniId, ownerId)
        } catch (failure: SQLException) {
            LOGGER.error("Opened gift content {} could not be read back", furniId, failure)
            null
        }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(PrizeBoxes::class.java)
        const val NEW_BOX_SQL = "INSERT INTO furni (player_id, definition_id, state) VALUES (?, ?, ?)"
        const val NEW_BOX_PRIZE_SQL =
            "INSERT INTO recycler_boxes (furni_id, prize_definition_id, chat_bubble_id) VALUES (?, ?, ?)"
    }
}
