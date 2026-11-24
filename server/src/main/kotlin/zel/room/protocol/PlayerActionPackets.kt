package zel.room.protocol

import zel.game.players.Player
import zel.game.players.Respect
import zel.protocol.generated.DropCarryItemInCodec
import zel.protocol.generated.HandItemReceivedOut
import zel.protocol.generated.HandItemReceivedOutCodec
import zel.protocol.generated.PassCarryItemInCodec
import zel.protocol.generated.RateFlatInCodec
import zel.protocol.generated.RespectPlayerInCodec
import zel.protocol.generated.RoomRatingOut
import zel.protocol.generated.RoomRatingOutCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.EntryStore
import zel.room.entry.RoomLeaving
import zel.room.life.RoomLife
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

class PlayerActionPackets(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val life: RoomLife,
    private val entries: EntryStore,
    private val player: (Int) -> Player?,
    private val vote: (roomId: Int, playerId: Int) -> Int,
) : PacketPackage {
    override val name = "player-actions"
    override val routes =
        listOf(
            PacketRoute(RespectPlayerInCodec) { session, m -> respect(session, m.userId) },
            PacketRoute(PassCarryItemInCodec) { session, m ->
                inRoom(session) { scope, _, unit -> pass(scope, unit, m.userId) }
            },
            PacketRoute(DropCarryItemInCodec) { session, _ ->
                inRoom(session) { scope, _, unit -> life.dropItem(scope, unit) }
            },
            PacketRoute(RateFlatInCodec) { session, m -> if (m.points == 1) rate(session) },
        )

    private fun respect(
        session: PacketSession,
        targetId: Int,
    ) {
        val giver = session.client.player ?: return
        val id = giver.profile.id
        if (targetId == id || giver.stats.tally.respectsLeft <= 0) return
        val roomId = registry.roomOf(id) ?: return
        if (registry.roomOf(targetId) != roomId) return
        player(targetId)?.let { Respect.give(giver, it) }
    }

    private fun pass(
        scope: RoomScope,
        unit: RoomUnit,
        targetId: Int,
    ) {
        val item = life.handItem(unit)
        val target = leaving.doorOf(scope.room).present[targetId]?.let { scope.room.units[it.unitIndex] }
        if (item == 0 || target == null || target === unit) return
        life.dropItem(scope, unit)
        scope.out.send(
            Lane.Now,
            Audience.Player(targetId),
            HandItemReceivedOutCodec,
            HandItemReceivedOut(unit.index, item),
        )
        life.holdItem(scope, target, item)
    }

    /** Votes are stored on the packet thread; only the answers go out under the room's lock. */
    private fun rate(session: PacketSession) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val (room, viewers) =
            registry.withPlayer(playerId) { scope, _ -> scope.room.info to playersIn(scope) } ?: return
        if (room.ownerId == playerId || entries.hasVoted(room.id, playerId)) return
        val score = vote(room.id, playerId)
        val mayVote = viewers.filter { it != room.ownerId && !entries.hasVoted(room.id, it) }.toSet()
        registry.withRoom(room.id) { scope ->
            scope.room.units.playerIds().filter { it in viewers }.forEach { viewer ->
                val rating = RoomRatingOut(score, viewer in mayVote)
                scope.out.send(Lane.Now, Audience.Player(viewer), RoomRatingOutCodec, rating)
            }
        }
    }

    private fun playersIn(scope: RoomScope): Set<Int> =
        scope.room.units
            .playerIds()
            .toSet()

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int, RoomUnit) -> Unit,
    ) {
        val id =
            session.client.player
                ?.profile
                ?.id ?: return
        registry.withPlayer(id) { scope, unit -> block(scope, id, unit) }
    }
}
