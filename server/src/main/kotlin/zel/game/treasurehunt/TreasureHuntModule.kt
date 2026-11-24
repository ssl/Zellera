package zel.game.treasurehunt

import zel.db.Database
import zel.game.items.Items
import zel.game.players.Players
import zel.protocol.treasurehunt.TreasureHuntMessages
import java.util.function.IntSupplier

class TreasureHuntModule(
    database: Database,
    items: Items,
    players: Players,
    unixTime: IntSupplier,
) {
    val hunts = TreasureHunts(HuntRecords(database), items, players, TreasureHuntMessages(), unixTime)

    init {
        hunts.reload()
    }
}
