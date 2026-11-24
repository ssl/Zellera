package zel.room.games

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.writeEach
import zel.game.players.figure.Figure
import zel.game.players.figure.LookValidation
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnections
import zel.protocol.generated.FigureUpdateOut
import zel.protocol.generated.FigureUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.tick.Audience
import zel.room.tick.Lane
import javax.sql.DataSource

class LiveGameHost(
    private val clients: ClientConnections,
    private val dataSource: DataSource,
    private val persist: (Runnable) -> Unit,
    private val appearanceChanged: (Int) -> Unit,
    private val clockSeconds: () -> Int,
    private val clockCounted: (RoomScope, Int, Int) -> Unit = { _, _, _ -> },
) : GameHost {
    override fun nowSeconds(): Int = clockSeconds()

    override fun figure(playerId: Int): PlayerFigure? =
        clients.onlinePlayer(playerId)?.profile?.let { PlayerFigure(it.figure, it.gender.name) }

    override fun outfitOver(
        look: String,
        outfit: String,
        gender: String,
        playerId: Int,
    ): String {
        val merged = Figure(look).dressedIn(Figure(outfit), KEPT_PARTS, REPLACED_PARTS).text
        val player = clients.onlinePlayer(playerId)
        return if (player != null && LookValidation.validateOnFballgate) {
            LookValidation.allowedLook(player, merged, gender)
        } else {
            merged
        }
    }

    override fun wear(
        scope: RoomScope,
        playerId: Int,
        look: String,
    ) {
        // A gate outfit over a long look can outgrow what a stored figure holds; the player keeps his own.
        if (!Figure.isWellFormed(look)) return
        val profile = clients.onlinePlayer(playerId)?.profile ?: return
        profile.figure = look
        persist(profile)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            FigureUpdateOutCodec,
            FigureUpdateOut(look, profile.gender.name),
        )
        appearanceChanged(playerId)
    }

    override fun clockCounted(
        scope: RoomScope,
        clock: RoomFurni,
        millis: Int,
    ) = clockCounted.invoke(scope, clock.id, millis)

    override fun record(game: GameRecord) {
        persist(Runnable { write(game) })
    }

    override fun counted(
        playerId: Int,
        goal: CountedAction,
        amount: Int,
    ) {
        persist(Runnable { PlayerActions.progress(playerId, goal, amount) })
    }

    override fun reached(
        playerId: Int,
        goal: CountedAction,
        size: Int,
    ) {
        persist(Runnable { PlayerActions.reached(playerId, goal, size) })
    }

    private fun write(game: GameRecord) {
        log.orFallback("saving the result of a ${game.game} in room ${game.roomId}", Unit) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val id =
                        connection.insertForId(INSERT_RESULT) {
                            int(game.roomId)
                            string(game.game)
                            int(game.startedAt)
                            int(game.endedAt)
                        } ?: return@allOrNothing null
                    connection.writeEach(INSERT_TEAM, game.teams) { team ->
                        int(id)
                        int(team.color)
                        int(team.points)
                    }
                    val lines = game.teams.flatMap { team -> team.players.map { team.color to it } }
                    connection.writeEach(INSERT_PLAYER, lines) { (team, player) ->
                        int(id)
                        int(player.playerId)
                        int(team)
                        int(player.score)
                    }
                    id
                }
            }
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LiveGameHost::class.java)
        const val INSERT_RESULT =
            "INSERT INTO room_game_results (room_id, game, started_at, ended_at) VALUES (?, ?, ?, ?)"
        const val INSERT_TEAM = "INSERT INTO room_game_result_teams (result_id, team, team_points) VALUES (?, ?, ?)"
        const val INSERT_PLAYER =
            "INSERT INTO room_game_result_players (result_id, player_id, team, score) VALUES (?, ?, ?, ?)"
        val KEPT_PARTS = setOf("hd", "hr", "ha", "he", "ea", "fa")
        val REPLACED_PARTS = setOf("ch", "ca", "cc", "cp", "lg", "wa", "sh")
    }
}
