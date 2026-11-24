package zel.game.polls

import zel.core.threading.GameThreads
import javax.sql.DataSource

class PollModule(
    dataSource: DataSource,
    threading: GameThreads,
    now: () -> Int,
) {
    val polls = Polls(PollStore(dataSource)).also(Polls::reload)
    val offers = PollOffers(polls, threading, now)
}
