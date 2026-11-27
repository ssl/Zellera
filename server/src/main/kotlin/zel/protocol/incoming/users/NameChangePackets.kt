package zel.protocol.incoming.users

import io.netty.util.AttributeKey
import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.db.SqlFailure
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.players.NameClaim
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.ChangePlayerNameInCodec
import zel.protocol.generated.ChangePlayerNameInRoomInCodec
import zel.protocol.generated.CheckPlayerNameInCodec
import zel.protocol.generated.PlayerNameChangedOut
import zel.protocol.generated.PlayerNameChangedOutCodec
import zel.protocol.outgoing.rooms.users.ChangeNameResult
import zel.protocol.outgoing.users.ChangeNameCheckResult
import zel.protocol.outgoing.users.ChangeNameCheckResult.NameVerdict
import zel.protocol.outgoing.users.UserData
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.signedIn
import zel.server.GameServices

class NameChangePackets(
    private val services: GameServices,
) : PacketPackage {
    private val names by lazy { NameClaim(services.database.queries) }

    /** A check is a click the database answers, so it gets the click gap. */
    private val checkCooldown = ClientCooldown("users.check-name", CHECK_GAP_MS)

    override val name = "name-change"

    override val routes =
        listOf(
            PacketRoute(CheckPlayerNameInCodec) { session, m -> check(session.client, m.name) },
            PacketRoute(ChangePlayerNameInCodec) { session, m -> confirm(session.client, m.name) },
            PacketRoute(ChangePlayerNameInRoomInCodec) { session, m -> confirm(session.client, m.name) },
        )

    private fun check(
        client: ClientConnection,
        requested: String,
    ) {
        val player = client.signedIn
        if (!player.stats.standing.mayChangeName) return
        if (!checkCooldown.acquire(client)) {
            // The client locks the window until it is answered, so a double click gets the answer it already had.
            val last = client.channel.attr(LAST_CHECK).get()
            if (last?.name == requested) client.send(last)
            return
        }
        val suggestions = ArrayList<String>()
        val verdict =
            when {
                requested.length < rules.playerNameMin -> {
                    NameVerdict.SHORT
                }

                requested.length > rules.playerNameMax -> {
                    NameVerdict.LONG
                }

                isTaken(player, requested) -> {
                    repeat(SUGGESTIONS) { suggestions += requested + services.random().nextInt(SUGGESTION_BOUND) }
                    NameVerdict.TAKEN
                }

                !passesFilter(player, requested) || !isSpelledWell(requested) -> {
                    NameVerdict.UNFIT
                }

                else -> {
                    player.stats.standing.checkedNewName = requested
                    NameVerdict.FREE
                }
            }
        val answer = ChangeNameCheckResult(verdict, requested, suggestions)
        client.channel.attr(LAST_CHECK).set(answer)
        client.send(answer)
    }

    private fun isTaken(
        player: Player,
        requested: String,
    ): Boolean = requested.equals(player.profile.name, ignoreCase = true) || exists(requested)

    private fun passesFilter(
        player: Player,
        requested: String,
    ): Boolean = checkNotNull(services.game.wordFilter).filter(requested, player).equals(requested, ignoreCase = true)

    /** The rule the site applies when a name is registered: plain letters, digits and the listed punctuation. */
    private fun isSpelledWell(name: String): Boolean {
        val punctuation = rules.playerNamePunctuation
        return name.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in punctuation }
    }

    private fun exists(name: String): Boolean = checkNotNull(services.game.players).readProfile(name) != null

    private fun confirm(
        client: ClientConnection,
        requested: String,
    ) {
        val player = client.signedIn
        val standing = player.stats.standing
        if (!standing.mayChangeName) return
        if (requested.equals(player.profile.name, ignoreCase = true)) {
            markNameChangeUsed(player)
            tell(client, player)
            countNamePicked(player)
            return
        }
        if (requested != standing.checkedNewName) return
        val oldName = player.profile.name
        if (!names.take(player.profile.id, requested)) {
            client.send(ChangeNameCheckResult(NameVerdict.TAKEN, requested))
            return
        }
        markNameChangeUsed(player)
        standing.checkedNewName = ""
        rename(player, requested)
        tell(client, player)
        record(player.profile.id, oldName, requested)
        countNamePicked(player)
    }

    private fun rename(
        player: Player,
        newName: String,
    ) {
        val oldName = player.profile.name
        player.profile.name = newName
        checkNotNull(services.game.players).renamed(player, oldName)
        player.profile.run()
        services.game.rooms.ownerRenamed(player.profile.id, newName)
        checkNotNull(services.game.guilds).ownedBy(player.profile.id).forEach { it.ownerName = newName }
    }

    private fun tell(
        client: ClientConnection,
        player: Player,
    ) {
        client.send(ChangeNameResult(player))
        if (player.inRoom) {
            player.rooms.nameChanged(player.profile.id)
        } else {
            client.send(
                PlayerNameChangedOutCodec,
                PlayerNameChangedOut(player.profile.id, OUTSIDE_A_ROOM, player.profile.name),
            )
        }
        player.messenger.connectionChanged(player, true, player.inRoom)
        client.send(UserData(player))
    }

    private fun markNameChangeUsed(player: Player) {
        player.stats.standing.mayChangeName = false
        try {
            services.database.queries.write(CLEAR_RENAME_SQL, player.profile.id)
        } catch (e: SqlFailure) {
            log.error("Clearing the rename grant of player {} failed", player.profile.id, e)
        }
    }

    private fun countNamePicked(player: Player) {
        services.game.achievements?.count(player, GameEvents.PLAYER_NAME_CHANGED)
    }

    private fun record(
        playerId: Int,
        oldName: String,
        newName: String,
    ) {
        try {
            services.database.queries.write(RECORD_SQL, playerId, oldName, newName, services.unixTimestamp())
        } catch (e: SqlFailure) {
            log.error("Recording the name change of player {} failed", playerId, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NameChangePackets::class.java)
        private const val OUTSIDE_A_ROOM = 0
        private const val SUGGESTIONS = 4
        private const val SUGGESTION_BOUND = 9999
        private const val CHECK_GAP_MS = 300L
        private val LAST_CHECK: AttributeKey<ChangeNameCheckResult> = AttributeKey.valueOf("name-check-answer")
        private const val RECORD_SQL =
            "INSERT INTO name_changes (player_id, old_name, new_name, changed_at) VALUES (?, ?, ?, ?)"
        private const val CLEAR_RENAME_SQL = "UPDATE player_standing SET may_change_name = 0 WHERE player_id = ?"
    }
}
