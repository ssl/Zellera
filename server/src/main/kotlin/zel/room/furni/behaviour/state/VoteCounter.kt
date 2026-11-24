package zel.room.furni.behaviour.state

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.StuffOnWire

class VoteCounter(
    private val part: StatePart,
) : Usable,
    StuffOnWire {
    override fun stuff(furni: RoomFurni): StuffData {
        val (frozen, votes) = read(furni)
        return SpecialStuff.of(furni, VOTES, StuffData.Value.Vote(if (frozen) "0" else "1", votes))
    }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        val (frozen, votes) = read(furni)
        if (frozen) part.of(scope.room).voted.remove(furni.id)
        StateShow.whole(scope, furni, write(!frozen, if (frozen) 0 else votes))
    }

    fun signed(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        sign: Int,
    ) {
        val (frozen, votes) = read(furni)
        if (frozen ||
            !part
                .of(scope.room)
                .voted
                .getOrPut(furni.id) { HashSet() }
                .add(playerId)
        ) {
            return
        }
        StateShow.whole(scope, furni, write(false, votes + sign))
    }

    private fun read(furni: RoomFurni): Pair<Boolean, Int> {
        val parts = furni.state.split(',')
        if (parts.size < 2) return true to 0
        return (parts[0] == "1") to (parts[1].toIntOrNull() ?: 0)
    }

    private fun write(
        frozen: Boolean,
        votes: Int,
    ) = "${if (frozen) "1" else "0"},$votes"

    private companion object {
        const val VOTES = 3
    }
}
