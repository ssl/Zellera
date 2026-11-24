package zel.game.earnings

import zel.game.items.Items
import zel.game.players.Player
import java.time.Clock
import javax.sql.DataSource

class EarningsModule(
    dataSource: DataSource,
    clock: Clock,
    online: (Int) -> Player?,
    items: () -> Items?,
) {
    val rewards = IncomeRewards(JdbcIncomeRewardStore(dataSource), clock, online, items)
    val wiredCredits = WiredCreditEarnings(rewards)
}
