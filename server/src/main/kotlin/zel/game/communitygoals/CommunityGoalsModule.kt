package zel.game.communitygoals

import zel.core.Configuration
import zel.db.Database
import zel.game.players.Players
import zel.protocol.communitygoals.CommunityGoalPackets
import zel.protocol.typed.PacketPackage
import java.util.function.IntSupplier

class CommunityGoalsModule(
    database: Database,
    configuration: Configuration,
    players: Players,
    unixTime: IntSupplier,
) {
    val tiered = TieredCommunityGoal(TieredGoalTable(database), unixTime)
    val concurrent = ConcurrentPlayersGoal(GoalClaims(database), configuration, players, unixTime)
    val packets: PacketPackage = CommunityGoalPackets(tiered, concurrent)
}
