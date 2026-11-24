package zel.game.commands

import zel.core.BuildInfo
import zel.core.CommandLog
import zel.core.Configuration
import zel.core.Texts
import zel.game.ServerInfo
import zel.game.permissions.Permissions
import zel.game.players.Players
import zel.room.port.RoomPort

@Suppress("LongParameterList") // the composition point of the chat commands
class CommandsModule(
    texts: Texts,
    config: Configuration,
    players: Players,
    rooms: () -> RoomPort,
    permissions: Permissions,
    commandLog: () -> CommandLog,
    info: ServerInfo,
) {
    private val targets = StaffRoomTarget(permissions, rooms) { players.find(it) }
    private val present = { playerId: Int -> rooms().roomOf(playerId) != null }

    private val self = SelfCommands(texts, rooms, present)

    private val roster: List<ChatCommand> =
        listOf(
            // This must stay: NOTICE (AGPL-3.0 section 7(b)) requires its notices in every version.
            About(config, players, rooms, "${BuildInfo.PRODUCT} ${info.version}", info.build, info.uptimeSeconds)
                .command(),
        ) + self.all() + RoomOwnerCommands(rooms).all() + StaffCommands(targets, rooms).all()

    val spoken: SpokenCommands = SpokenCommands(roster, commandLog, self::clubMoveAlias)
}
