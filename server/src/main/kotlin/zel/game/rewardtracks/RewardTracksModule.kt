package zel.game.rewardtracks

import zel.game.achievements.AchievementTally
import zel.game.bots.Bots
import zel.game.catalog.Catalog
import zel.game.items.Items
import zel.game.pets.PetsModule
import zel.protocol.rewardtracks.RewardTrackMessages
import zel.protocol.rewardtracks.RewardTrackPackets
import zel.protocol.typed.PacketPackage
import java.util.concurrent.Executor
import javax.sql.DataSource

@Suppress("LongParameterList") // the composition point: every service a prize reaches
class RewardTracksModule(
    dataSource: DataSource,
    writes: Executor,
    items: Items,
    pets: PetsModule,
    bots: Bots,
    catalog: Catalog,
    tally: AchievementTally,
) {
    private val prizes = PrizeHandout(items, pets, bots, catalog)

    val service =
        RewardTrackService(
            RewardTrackStore(dataSource),
            writes,
            prizes::handOut,
            prizes::canPay,
            RewardTrackMessages(),
            tally,
        )
    val packets: PacketPackage = RewardTrackPackets(service)
}
