package zel.game.rewardtracks

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.rules.ActiveRules
import zel.core.rules.GameRules
import zel.db.SqlFailure
import zel.game.achievements.AchievementTally
import zel.game.achievements.GameEvents
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.players.Player
import zel.game.players.PlayerSaveQueue
import zel.game.players.PlayerStates
import zel.game.quests.CountedAction
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.outgoing.users.UserCurrency
import zel.protocol.rewardtracks.RewardTrackEntry
import zel.protocol.rewardtracks.RewardTrackMessages
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/** A player's state stays held until its [writeExecutor] writes are in. */
class RewardTrackService internal constructor(
    private val store: RewardTrackStore?,
    writeExecutor: Executor,
    private val handout: Handout,
    private val canPay: (RewardTrackDefinition.Prize) -> Boolean,
    private val messages: RewardTrackMessages,
    private val tally: AchievementTally,
) {
    fun interface Handout {
        fun handOut(
            player: Player,
            prize: RewardTrackDefinition.Prize,
        )
    }

    private class Standing(
        val byTrack: MutableMap<String, PlayerTrack>,
    ) : PlayerStates.Held() {
        fun on(track: RewardTrackDefinition): PlayerTrack = byTrack.computeIfAbsent(track.id) { PlayerTrack() }
    }

    @Volatile
    private var tracks: List<RewardTrackDefinition> = emptyList()
    private val standings =
        PlayerStates<Standing>(PlayerSaveQueue(writeExecutor::execute), { true }) { playerId, _ ->
            Standing(store?.playerTracks(playerId) ?: ConcurrentHashMap())
        }

    private val rules: GameRules get() = ActiveRules.get()

    private fun offer(): PremiumOffer? {
        val pass = rules.premiumPass
        if (rules.premiumForEveryone || !pass.onSale) return null
        return PremiumOffer(pass.pointsFactor, pass.instantPoints, pass.premium, pass.credits)
    }

    private fun premiumOf(state: PlayerTrack): Boolean = rules.premiumForEveryone || state.hasPass()

    /** Tracks the database cannot give leave the list as it was. */
    fun load() {
        val loaded =
            try {
                store?.definitions { rules.trackEnabled(it) } ?: return
            } catch (failure: SqlFailure) {
                LOGGER.warn("Reward tracks could not be read", failure)
                return
            }
        setTracks(loaded)
        LOGGER.info("{} reward tracks loaded", loaded.size)
    }

    fun setTracks(definitions: List<RewardTrackDefinition>) {
        tracks = definitions.toList()
    }

    fun track(trackId: String): RewardTrackDefinition? = tracks.firstOrNull { it.id == trackId }

    fun stateFor(
        playerId: Int,
        track: RewardTrackDefinition,
    ): PlayerTrack = standings.of(playerId).on(track)

    /** The state goes once its writes are in, so a quick relog never reads a paid prize as unclaimed. */
    fun unload(playerId: Int) = standings.unload(playerId)

    fun sendTracks(
        client: ClientConnection,
        playerId: Int,
    ) {
        val offer = offer()
        val standing =
            try {
                standings.of(playerId)
            } catch (failure: SqlFailure) {
                LOGGER.error("The reward tracks of player {} could not be read", playerId, failure)
                return
            }
        val listing =
            tracks.map { track ->
                val state = standing.on(track)
                RewardTrackEntry(
                    track,
                    state.points(),
                    premiumOf(state),
                    state.progressSnapshot(),
                    state.claimedSnapshot(),
                    offer,
                )
            }
        messages.tracks(client, false, listing, false)
    }

    fun progress(
        player: Player,
        action: CountedAction,
        amount: Int,
    ) {
        if (amount <= 0) return
        val boost = rules.premiumPass.pointsFactor
        val playerId = player.profile.id
        standings.change(playerId) { standing ->
            for (track in tracks) {
                val state = standing.on(track)
                val factor = if (state.hasPass()) boost else 1.0
                for (task in track.tasks) {
                    if (task.goal != action || (task.premium && !premiumOf(state))) continue
                    val moved = state.advance(task, amount, factor) ?: continue
                    standings.write(playerId) { store?.saveProgress(playerId, track.id, task.id, moved) }
                    player.client?.let { messages.progress(it, track.id, task.id, moved.count, moved.points) }
                }
            }
        }
    }

    fun claim(
        player: Player,
        trackId: String,
        prizeId: String,
    ): ClaimResult {
        val playerId = player.profile.id
        val track = track(trackId)
        val prize = track?.prize(prizeId)
        val outcome =
            when {
                track == null -> PlayerTrack.Claim(ClaimResult.TRACK_NOT_FOUND)
                prize == null -> PlayerTrack.Claim(ClaimResult.REWARD_NOT_FOUND)
                else -> claimOnceOrFail(playerId, track, prize)
            }
        player.client?.let { messages.claimResult(it, trackId, prizeId, outcome.result.code) }
        if (outcome.result == ClaimResult.OK && prize != null) {
            handOutLogged(player, prize)
            if (outcome.completedTrack) tally.add(player, GameEvents.REWARD_TRACK_COMPLETED, 1)
        }
        return outcome.result
    }

    fun buyPass(
        player: Player,
        trackId: String,
    ): PassPurchase {
        val sale =
            try {
                sell(player, trackId)
            } catch (failure: SqlFailure) {
                LOGGER.error("The pass of track {} for player {} was not sold", trackId, player.profile.id, failure)
                Sale(PassPurchase.FAILED)
            }
        player.client?.let { client ->
            messages.passResult(client, trackId, sale.result.code, sale.points)
            if (sale.result == PassPurchase.OK) {
                client.send(UserCredits(player))
                client.send(UserCurrency(player))
            }
        }
        return sale.result
    }

    private class Sale(
        val result: PassPurchase,
        val points: Int = 0,
    )

    private fun sell(
        player: Player,
        trackId: String,
    ): Sale {
        val playerId = player.profile.id
        val track = track(trackId) ?: return Sale(PassPurchase.TRACK_NOT_FOUND)
        if (rules.premiumForEveryone) return Sale(PassPurchase.LANE_IS_EVERYONES)
        if (stateFor(playerId, track).hasPass()) return Sale(PassPurchase.ALREADY_OWNED)
        val offer = offer() ?: return Sale(PassPurchase.NOT_ON_SALE)
        val paid = take(player, offer)
        if (paid != PassPurchase.OK) return Sale(paid)
        return Sale(PassPurchase.OK, grantPass(playerId, track, offer.instantPoints))
    }

    /** Premium currency that cannot be taken give the credits back. */
    private fun take(
        player: Player,
        offer: PremiumOffer,
    ): PassPurchase {
        val wallet = player.wallet
        if (offer.credits > 0 && !wallet.takeCredits(offer.credits)) return PassPurchase.NOT_ENOUGH_CREDITS
        val operation = LedgerEntry.newKey("reward-track-pass")
        if (offer.premium > 0 &&
            !wallet.takePoints(EconomyLedger.PREMIUM, offer.premium, PASS_REASON, operation, null)
        ) {
            if (offer.credits > 0) wallet.changeCredits(offer.credits, PASS_REASON)
            return PassPurchase.NOT_ENOUGH_PREMIUM
        }
        return PassPurchase.OK
    }

    private fun grantPass(
        playerId: Int,
        track: RewardTrackDefinition,
        instantPoints: Int,
    ): Int =
        standings.change(playerId) { standing ->
            val points = standing.on(track).buyPass(instantPoints)
            standings.write(playerId) { store?.savePass(playerId, track.id, points) }
            points
        }

    /** A state that cannot be read answers FAILED instead of paying again what it does not know was claimed. */
    private fun claimOnceOrFail(
        playerId: Int,
        track: RewardTrackDefinition,
        prize: RewardTrackDefinition.Prize,
    ): PlayerTrack.Claim =
        try {
            claimOnce(playerId, track, prize)
        } catch (failure: SqlFailure) {
            LOGGER.error("Reward track claim {}/{} of player {} failed", track.id, prize.id, playerId, failure)
            PlayerTrack.Claim(ClaimResult.FAILED)
        }

    private fun claimOnce(
        playerId: Int,
        track: RewardTrackDefinition,
        prize: RewardTrackDefinition.Prize,
    ): PlayerTrack.Claim =
        standings.change(playerId) { standing ->
            val state = standing.on(track)
            val outcome = state.claim(track, prize, premiumOf(state), canPay)
            if (outcome.result == ClaimResult.OK) {
                standings.write(playerId) { store?.saveClaim(playerId, track.id, prize.id) }
            }
            outcome
        }

    /** A prize that fails to hand out is logged; the claim stays stored and the track's completion still counts. */
    @Suppress("TooGenericExceptionCaught") // a failing prize must not undo the stored claim
    private fun handOutLogged(
        player: Player,
        prize: RewardTrackDefinition.Prize,
    ) {
        try {
            handout.handOut(player, prize)
        } catch (failure: RuntimeException) {
            LOGGER.error("Prize {} of player {} was not handed out", prize.id, player.profile.id, failure)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(RewardTrackService::class.java)
        const val PASS_REASON = "reward_track.pass"
    }
}
