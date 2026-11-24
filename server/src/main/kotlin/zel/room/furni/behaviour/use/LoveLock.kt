package zel.room.furni.behaviour.use

import zel.game.items.StateFields
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.FriendFurniCancelLockOut
import zel.protocol.generated.FriendFurniCancelLockOutCodec
import zel.protocol.generated.FriendFurniOtherLockConfirmedOut
import zel.protocol.generated.FriendFurniOtherLockConfirmedOutCodec
import zel.protocol.generated.FriendFurniStartConfirmationOut
import zel.protocol.generated.FriendFurniStartConfirmationOutCodec
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.behaviour.state.Near
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.Placeable
import zel.room.furni.place.StuffOnWire
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.ConcurrentHashMap

/** Engraved by the lock's owner and one other player, both standing beside it; an engraving is for good. */
class LoveLock(
    private val player: (Int) -> Player?,
) : Usable,
    Placeable,
    StuffOnWire {
    private class Proposal(
        val ownerId: Int,
    ) {
        var partnerId = 0
        var ownerAgreed = false
        var partnerAgreed = false

        fun pair(): List<Int> = listOfNotNull(ownerId, partnerId.takeIf { it > 0 })

        fun otherOf(playerId: Int): Int? =
            when (playerId) {
                ownerId -> partnerId
                partnerId -> ownerId
                else -> null
            }
    }

    private val proposals = ConcurrentHashMap<Int, Proposal>()

    override fun stuff(furni: RoomFurni): StuffData {
        val engraved = StateFields.read(furni.state)
        val strings =
            if (engraved == null) {
                listOf(OPEN, "", "", "", "", "")
            } else {
                listOf(LOCKED) + listOf(ONE, TWO, LOOK_ONE, LOOK_TWO).map(engraved::text) + shownDay(engraved.text(DAY))
            }
        return SpecialStuff.strings(furni, strings)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        proposals.remove(furni.rowId)?.pair()?.forEach { close(scope, it, furni) }
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (isEngraved(furni) || !furni.isSpendable || !beside(scope, furni, playerId)) return
        val proposal = proposals[furni.rowId]?.takeIf { stands(scope, furni, it) }
        when {
            playerId == furni.ownerId && proposal == null -> {
                proposals[furni.rowId] = Proposal(playerId)
                ask(scope, playerId, furni, first = true)
            }

            proposal != null && proposal.partnerId == 0 && playerId != proposal.ownerId -> {
                proposal.partnerId = playerId
                ask(scope, playerId, furni, first = false)
            }
        }
    }

    fun confirm(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        yes: Boolean,
    ) {
        val proposal = proposals[furni.rowId] ?: return
        val other = proposal.otherOf(playerId) ?: return
        if (!yes || isEngraved(furni)) {
            proposals.remove(furni.rowId)
            if (other > 0) close(scope, other, furni)
            return
        }
        if (playerId == proposal.ownerId) proposal.ownerAgreed = true else proposal.partnerAgreed = true
        if (proposal.ownerAgreed && proposal.partnerAgreed) {
            proposals.remove(furni.rowId)
            engrave(scope, furni, proposal)
            proposal.pair().forEach { close(scope, it, furni) }
        } else if (other > 0) {
            val told = FriendFurniOtherLockConfirmedOut(furni.id)
            scope.out.send(Lane.Now, Audience.Player(other), FriendFurniOtherLockConfirmedOutCodec, told)
        }
    }

    private fun engrave(
        scope: RoomScope,
        furni: RoomFurni,
        proposal: Proposal,
    ) {
        val one = player(proposal.ownerId)?.profile ?: return
        val two = player(proposal.partnerId)?.profile ?: return
        if (proposal.ownerId != furni.ownerId || !stands(scope, furni, proposal)) return
        val engraving =
            StateFields
                .write()
                .text(ONE, one.name)
                .text(TWO, two.name)
                .text(LOOK_ONE, one.figure)
                .text(LOOK_TWO, two.figure)
                .text(DAY, GameZone.today().toString())
        StateShow.whole(scope, furni, engraving.state())
        proposal.pair().forEach { PlayerActions.progress(it, CountedAction.FRIEND_FURNI_LOCKED, 1) }
    }

    private fun shownDay(stored: String): String =
        try {
            SHOWN_DAY.format(LocalDate.parse(stored))
        } catch (_: DateTimeParseException) {
            stored
        }

    private fun isEngraved(furni: RoomFurni): Boolean = StateFields.read(furni.state) != null

    private fun beside(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ): Boolean =
        scope.room.units
            .ofPlayer(playerId)
            ?.let { Near.adjacent(furni, it.tile) } == true

    /** A proposal lapses as soon as one of its players has left the lock's side. */
    private fun stands(
        scope: RoomScope,
        furni: RoomFurni,
        proposal: Proposal,
    ): Boolean = proposal.pair().all { beside(scope, furni, it) }

    private fun ask(
        scope: RoomScope,
        playerId: Int,
        furni: RoomFurni,
        first: Boolean,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        FriendFurniStartConfirmationOutCodec,
        FriendFurniStartConfirmationOut(furni.id, first),
    )

    private fun close(
        scope: RoomScope,
        playerId: Int,
        furni: RoomFurni,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        FriendFurniCancelLockOutCodec,
        FriendFurniCancelLockOut(furni.id),
    )

    private companion object {
        const val LOCKED = "1"
        const val OPEN = "0"
        const val ONE = "one"
        const val TWO = "two"
        const val LOOK_ONE = "lookOne"
        const val LOOK_TWO = "lookTwo"
        const val DAY = "day"
        val SHOWN_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }
}
