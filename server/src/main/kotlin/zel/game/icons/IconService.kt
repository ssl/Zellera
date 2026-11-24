package zel.game.icons

import zel.content.ClientStrings
import zel.game.achievements.AchievementTally
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.generated.PlayerIconStatusChangedOut
import zel.protocol.generated.PlayerIconStatusChangedOutCodec
import zel.protocol.outgoing.icons.PlayerIcons
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class IconService(
    dataSource: DataSource,
    private val tally: AchievementTally,
) {
    private val store = IconStore(dataSource)

    enum class Action { CLAIM, FAVORITE, UNFAVORITE }

    data class Outcome(
        val roster: IconRoster,
        val changed: List<IconRoster.Icon>,
        val newlyOwned: Int = 0,
    )

    @Throws(SQLException::class)
    fun rosterOf(player: Player): IconRoster = rosterOf(player.profile.id, allUnlocked(player))

    @Throws(SQLException::class)
    fun rosterOf(
        playerId: Int,
        allUnlocked: Boolean,
    ): IconRoster = store.read(playerId) { it.roster(allUnlocked) }

    @Throws(SQLException::class)
    fun perform(
        player: Player,
        action: Action,
        id: Int,
    ): Outcome {
        val outcome = perform(player.profile.id, allUnlocked(player), action, id)
        announce(player, outcome)
        countCollected(player, outcome)
        if (action == Action.CLAIM) IconAchievements.claimed(player, outcome.roster, id, tally)
        return outcome
    }

    @Throws(SQLException::class)
    fun perform(
        playerId: Int,
        allUnlocked: Boolean,
        action: Action,
        id: Int,
    ): Outcome =
        changeUnderLock(playerId, allUnlocked) { rows, prior ->
            rows.saveState(id, stateAfter(action, prior.requireIcon(id).state), action == Action.CLAIM)
        }

    private fun stateAfter(
        action: Action,
        state: IconState,
    ): IconState {
        val fits =
            when (action) {
                Action.CLAIM -> state == IconState.CLAIMABLE
                Action.FAVORITE, Action.UNFAVORITE -> state.owned
            }
        if (!fits) throw IconRejected(IconRejected.UNAVAILABLE)
        return if (action == Action.FAVORITE) IconState.FAVORITE else IconState.OWNED
    }

    /** One already in hand stays as it is. */
    @Throws(SQLException::class)
    fun grant(
        playerId: Int,
        allUnlocked: Boolean,
        id: Int,
    ): Outcome =
        changeUnderLock(playerId, allUnlocked) { rows, prior ->
            if (!prior.requireIcon(id).state.owned) rows.saveState(id, IconState.OWNED, true)
        }

    /** Gives the icons of a paid purchase; a second buy that raced the first is rejected, so the caller refunds it. */
    @Throws(SQLException::class)
    fun grantPurchase(
        playerId: Int,
        allUnlocked: Boolean,
        bought: IconPurchase,
    ): Outcome =
        changeUnderLock(playerId, allUnlocked) { rows, prior ->
            if (bought.ids.any { prior.requireIcon(it).state.countsForSet }) {
                throw IconRejected(IconRejected.UNAVAILABLE)
            }
            bought.ids.forEach { rows.saveState(it, IconState.OWNED, false) }
        }

    /** Applies [write] under the player's lock; a set reward the write opened is stored as claimable and unseen. */
    private fun changeUnderLock(
        playerId: Int,
        allUnlocked: Boolean,
        write: (IconStore.PlayerRows, IconRoster) -> Unit,
    ): Outcome =
        store.exclusively(playerId) { rows ->
            val prior = rows.roster(allUnlocked)
            write(rows, prior)
            val after = rows.roster(allUnlocked)
            val changed = after.changedSince(prior)
            val opened =
                changed.filter {
                    it.state == IconState.CLAIMABLE && prior.requireIcon(it.id).state == IconState.UNAVAILABLE
                }
            opened.forEach { rows.saveState(it.id, IconState.CLAIMABLE, true) }
            val roster = if (opened.isEmpty()) after else rows.roster(allUnlocked)
            Outcome(roster, changed, IconRoster.newlyOwned(prior, changed))
        }

    fun countCollected(
        player: Player?,
        outcome: Outcome?,
    ) {
        if (player == null || outcome == null || outcome.newlyOwned <= 0) return
        ClientStrings[ClientStrings.ICON_COLLECTED_ACHIEVEMENT]?.let { tally.add(player, it, outcome.newlyOwned) }
    }

    @Throws(SQLException::class)
    fun use(
        playerId: Int,
        allUnlocked: Boolean,
        id: Int,
    ): Boolean =
        store.exclusively(playerId) { rows ->
            val icon = rows.roster(allUnlocked).icons[id]
            if (icon == null || !icon.state.owned) return@exclusively false
            // an icon owned without a row (by default or permission) gets its row here
            rows.saveState(id, icon.state, false)
            rows.markUsed(id, Instant.now().toEpochMilli())
            true
        }

    @Throws(SQLException::class)
    fun markSeen(
        playerId: Int,
        ids: List<Int>,
    ) {
        if (ids.size > MAX_MARKED_SEEN) throw IconRejected(IconRejected.UNKNOWN)
        store.markSeen(playerId, ids)
    }

    companion object {
        private const val MAX_MARKED_SEEN = 1000

        fun allUnlocked(player: Player): Boolean = player.hasPermission(Privilege.ICONS_OWN_ALL)

        fun announceStates(
            player: Player,
            outcome: Outcome,
        ) {
            outcome.changed.forEach {
                player.client?.send(PlayerIconStatusChangedOutCodec, PlayerIconStatusChangedOut(it.id, it.state.code))
            }
        }

        fun announce(
            player: Player,
            outcome: Outcome,
        ) {
            announceStates(player, outcome)
            player.client?.send(PlayerIcons(outcome.roster))
            val unseen = outcome.roster.unseen.toIntArray()
            if (unseen.isNotEmpty()) {
                player.client?.send(AddRoomItem(unseen, UnseenCategory.ICON))
            }
        }
    }
}
