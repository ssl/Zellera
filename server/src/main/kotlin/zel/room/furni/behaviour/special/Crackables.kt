package zel.room.furni.behaviour.special

import zel.game.items.CrackableKind
import zel.game.items.Item
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.Near
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.StuffOnWire
import zel.util.SECONDS_PER_DAY
import kotlin.random.Random

class Crackables(
    private val part: SpecialPart,
) {
    val plain = Crackable(Kind.Keepsake)
    val master = Crackable(Kind.Shared)
    val monster = Crackable(Kind.Monster)
    val subscription = Crackable(Kind.Voucher)

    enum class Kind(
        /** Anyone in the room hits it, not only its owner. */
        val open: Boolean = false,
        /** It closes again after a crack instead of leaving the room. */
        val closesAgain: Boolean = false,
        /** Damage grows back while it stands. */
        val regrows: Boolean = false,
        /** It is opened from any tile; the others are hit from beside them. */
        val fromAnywhere: Boolean = false,
        /** Its prize takes its place in the room; else it goes to an inventory. */
        val leavesPrize: Boolean = false,
    ) {
        Keepsake(leavesPrize = true),
        Shared(open = true, closesAgain = true),
        Monster(regrows = true, leavesPrize = true),
        Voucher(fromAnywhere = true),
    }

    inner class Crackable(
        val kind: Kind,
    ) : Usable,
        Toggleable,
        StuffOnWire {
        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) {
            val unit = scope.room.units.ofPlayer(playerId) ?: return
            if (!furni.isSpendable || furni.id in part.of(scope.room).cracked) return
            if (!kind.fromAnywhere && !Near.adjacent(furni, unit.tile)) {
                Near.walkBeside(scope, part.hooks, furni, unit)
                return
            }
            val crackable = part.host.crackable(furni.definition.id) ?: return
            if (!kind.open && furni.ownerId != playerId) return
            val worn = part.hooks.effect(unit)
            val own = if (part.hooks.male(unit)) furni.definition.effectMale else furni.definition.effectFemale
            // A player already wearing the effect the furni itself gives does not hit it.
            if (own > 0 && own == worn) return
            if (crackable.requiredEffect > 0 && worn != crackable.requiredEffect) return
            hit(scope, furni, playerId, crackable)
        }

        override fun toggle(
            scope: RoomScope,
            furni: RoomFurni,
            backward: Boolean,
        ): Boolean = false

        override fun toggleRandom(
            scope: RoomScope,
            furni: RoomFurni,
            random: Random,
        ): Boolean = false

        override fun stuff(furni: RoomFurni): StuffData {
            val target = part.host.crackable(furni.definition.id)?.hitsNeeded ?: 0
            val hits = hits(furni.state)
            val shown = if (hits <= 0) "" else stage(hits, target, furni.definition.stateCount).toString()
            return SpecialStuff.of(furni, SpecialStuff.CRACKABLE, StuffData.Value.Crackable(shown, hits, target))
        }

        private fun hit(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            crackable: CrackableKind,
        ) {
            val hits = hits(furni.state) + 1
            StateShow.data(scope, furni, hits.toString())
            progress(playerId, crackable.hitAchievement)
            if (hits < crackable.hitsNeeded) return
            progress(playerId, crackable.crackedAchievement)
            if (kind.closesAgain) closeAgain(scope, furni, playerId) else open(scope, furni, crackable)
        }

        private fun closeAgain(
            scope: RoomScope,
            furni: RoomFurni,
            winnerId: Int,
        ) {
            part.of(scope.room).cracked += furni.id
            part.later(scope, furni, OPEN_MILLIS.toLong()) { later, whole ->
                part.of(later.room).cracked -= whole.id
                StateShow.data(later, whole, UNHIT)
                part.host.crackPrize(whole.definition.id)?.let { prize ->
                    part.persist(Runnable { part.host.giveFurni(winnerId, prize) })
                }
            }
        }

        /** What it held is its owner's, and only once its row is gone. */
        private fun open(
            scope: RoomScope,
            furni: RoomFurni,
            crackable: CrackableKind,
        ) {
            val ownerId = furni.ownerId
            val roomId = scope.room.info.id
            val prize = part.host.crackPrize(furni.definition.id)
            val spot = furni.anchor?.takeIf { kind.leavesPrize }?.let { SpecialPart.Spot(it, furni.z, furni.direction) }
            val shownAt = scope.now() + OPEN_MILLIS
            part.spend(scope, furni, OPEN_MILLIS, picker = NO_PICKER) {
                val membership = crackable.membership
                if (membership != null && crackable.givesMembership) {
                    part.host.membership(ownerId, membership, crackable.membershipDays * SECONDS_PER_DAY)
                }
                when {
                    prize == null -> Unit
                    spot != null -> place(roomId, ownerId, prize, spot, shownAt)
                    else -> part.host.giveFurni(ownerId, prize)
                }
            }
        }
    }

    fun tick(scope: RoomScope) {
        val monsters = scope.room.furni.selecting(REGROWING) { (it.behaviour as? Crackable)?.kind?.regrows == true }
        if (monsters.isEmpty()) return
        val special = part.of(scope.room)
        monsters.forEach { regrow(scope, special, it) }
    }

    /** One hit grows back every [REGROW_MILLIS], whether the monster is being hit or not. */
    private fun regrow(
        scope: RoomScope,
        special: SpecialRoom,
        monster: RoomFurni,
    ) {
        val hits = hits(monster.state)
        if (hits <= 0) return
        val since = special.healed.getOrPut(monster.id) { scope.now() }
        if (scope.now() - since < REGROW_MILLIS || monster.id in special.cracked) return
        special.healed[monster.id] = scope.now()
        StateShow.data(scope, monster, (hits - 1).toString())
    }

    /** Runs on the persistence executor; the prize shows once the opened furni has left the players' screens. */
    private fun place(
        roomId: Int,
        ownerId: Int,
        prize: Item,
        spot: SpecialPart.Spot,
        shownAt: Long,
    ) {
        val id = part.host.createFurni(ownerId, prize, UNHIT)
        if (id <= 0) return
        part.host.putInRoom(id, roomId, spot.anchor.x, spot.anchor.y, spot.z, spot.direction)
        val name = part.host.playerName(ownerId)
        val made = SpecialPart.NewFurni(id, prize)
        part.post(roomId) { scope ->
            part.at(scope, shownAt) { later -> part.arrive(later, ownerId, name, made, spot, UNHIT) }
        }
    }

    private fun progress(
        playerId: Int,
        achievement: String,
    ) {
        if (achievement.isNotEmpty()) part.hooks.progress(playerId, achievement, 1)
    }

    companion object {
        /** How long the client keeps an opened crackable on screen before it removes it. */
        const val OPEN_MILLIS = 1590
        const val NO_PICKER = -1

        private const val UNHIT = "0"

        /** Names the kept list of crackables that grow back. */
        private val REGROWING = Any()
        private const val REGROW_MILLIS = 30_000L

        fun hits(state: String): Int = state.trim().toIntOrNull() ?: 0

        /** The hits spread evenly over the furni's [stages]; the last hit shows the last one. */
        fun stage(
            hits: Int,
            target: Int,
            stages: Int,
        ): Int =
            if (hits <= 0 || target <= 0 || stages <= 0) {
                0
            } else {
                (minOf(hits, target).toLong() * stages / target).toInt()
            }
    }
}
