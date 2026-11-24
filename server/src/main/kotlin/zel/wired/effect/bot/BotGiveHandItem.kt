package zel.wired.effect.bot

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import kotlin.math.abs

class BotGiveHandItem(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val errands: BotErrands,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val item = (settings.ints.firstOrNull() ?: return).coerceAtLeast(0)
        val targets = Targets.units(firing, this, USER_SLOT)
        if (BotChoice.unnamed(settings)) {
            // No bot named: each user gets the item in hand at once, item 0 empties it. A bot as the user (clicked
            // user) holds it until a box empties it; pets get none.
            targets.filter { it.kind !is UnitKind.Pet }.forEach { errands.give(firing.scope, it, item) }
            return
        }
        val users = targets.filter { it.kind is UnitKind.Player }
        val chosen = BotChoice.bots(firing, this, bots)
        if (chosen.isEmpty()) return
        // Each user takes the nearest bot. A bot taken by several users shows the item once for each of them and
        // then serves only the farthest one.
        val taken = LinkedHashMap<Int, MutableList<RoomUnit>>()
        users.forEach { user ->
            val nearest = chosen.minBy { (_, unit) -> distance(unit, user) }
            taken.getOrPut(nearest.second.index) { ArrayList() } += user
        }
        chosen.forEach { (bot, unit) ->
            val mine = taken[unit.index] ?: return@forEach
            repeat(mine.size - 1) { errands.show(firing.scope, unit, item) }
            errands.serve(firing.scope, bot, unit, mine.maxBy { distance(unit, it) }, item)
        }
    }

    private fun distance(
        a: RoomUnit,
        b: RoomUnit,
    ): Int = maxOf(abs(a.tile.x - b.tile.x), abs(a.tile.y - b.tile.y))

    private companion object {
        const val USER_SLOT = 1
    }
}
