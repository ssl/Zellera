package zel.room.unit.bot

import zel.game.bots.Bot
import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.pets.Pet
import zel.game.players.Player
import zel.util.MILLIS_PER_SECOND
import java.sql.Connection
import java.sql.ResultSet
import kotlin.random.Random

@Suppress("TooManyFunctions") // one per server service bots and pets use
interface UnitHost {
    fun player(id: Int): Player? = null

    fun bot(row: ResultSet): Bot? = null

    fun pet(row: ResultSet): Pet? = null

    fun playerName(id: Int): String = ""

    fun text(
        key: String,
        default: String,
    ): String = default

    fun filter(
        player: Player,
        text: String,
    ): String = text

    fun censor(text: String): String = text

    fun config(
        key: String,
        default: Int,
    ): Int = default

    fun butlerServes(): Map<String, Int> = emptyMap()

    data class Visit(
        val roomName: String,
        val timestamp: Int,
    )

    fun roomVisits(
        roomId: Int,
        since: Int,
        excludeName: String,
    ): List<Visit> = emptyList()

    fun textOf(
        key: String,
        default: String,
    ): String = default

    fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    ) {}

    fun saddle(definitionId: Int): Item? = null

    /** Creates a furni of [definition] for [playerId] on [connection], inside the caller's transaction. */
    fun newFurni(
        connection: Connection,
        playerId: Int,
        definition: Item,
    ): OwnedFurni? = null

    val random: Random get() = Random.Default

    fun nowSeconds(): Int = (System.currentTimeMillis() / MILLIS_PER_SECOND).toInt()

    companion object {
        val None: UnitHost = object : UnitHost {}
    }
}
