package zel.wired.effect.user

import zel.room.tick.Audience
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class VisualOut(
    private val firing: Firing,
    private val box: WiredBox,
    private val everyone: Boolean,
) {
    fun units(send: (List<Int>, Audience) -> Unit) {
        val indexes = Targets.units(firing, box, TARGET_USER_SLOT).map { it.index }
        if (indexes.isNotEmpty()) audiences().forEach { send(indexes, it) }
    }

    fun furni(send: (List<Int>, Audience) -> Unit) {
        val ids = Targets.furni(firing, box).map { if (it.isWall) -it.id else it.id }
        if (ids.isNotEmpty()) audiences().forEach { send(ids, it) }
    }

    private fun audiences(): List<Audience> =
        if (everyone) {
            listOf(Audience.Room)
        } else {
            UserTargets.players(firing, box, AUDIENCE_SLOT).map { (_, playerId) -> Audience.Player(playerId) }
        }

    private companion object {
        const val AUDIENCE_SLOT = 0
        const val TARGET_USER_SLOT = 1
    }
}
