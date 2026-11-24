package zel.wired.chest

import zel.core.Configuration
import zel.game.earnings.WiredCreditEarnings
import zel.game.items.ItemServices
import zel.net.clients.ClientConnections
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import javax.sql.DataSource

@Suppress("LongParameterList") // the server services a chest reaches
class ChestServices(
    val dataSource: DataSource,
    val clients: ClientConnections,
    val config: Configuration,
    val persist: (Runnable) -> Unit,
    val writer: WiredWriter,
    val items: () -> ItemServices?,
    val earnings: () -> WiredCreditEarnings?,
    val progress: (Int, String, Int) -> Unit,
    val poster: RoomPoster,
    val filterText: (Int, String) -> String = { _, text -> text },
    /** Achievement work for a player, after that player's earlier achievement work. */
    val later: (playerId: Int, job: Runnable) -> Unit = { _, job -> persist(job) },
)
