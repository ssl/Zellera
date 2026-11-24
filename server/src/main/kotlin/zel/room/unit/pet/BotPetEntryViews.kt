package zel.room.unit.pet

import zel.protocol.generated.RoomUnitsOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.bot.RoomBots

class BotPetEntryViews(
    private val bots: RoomBots,
    private val pets: RoomPets,
    private val next: EntryViews = EntryViews.None,
) : EntryViews by next {
    override fun otherUnits(
        scope: RoomScope,
        playerId: Int,
    ): Boolean {
        val others = next.otherUnits(scope, playerId)
        val to = Audience.Player(playerId)
        val present = listOfNotNull(bots.entries(scope), pets.entries(scope))
        present.forEach { scope.out.send(Lane.Now, to, RoomUnitsOutCodec, it) }
        return others || present.isNotEmpty()
    }

    override fun afterBurst(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        next.afterBurst(scope, unit, playerId)
        bots.dances(scope, playerId)
    }
}
