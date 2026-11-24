package zel.protocol.handshake

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.db.Sql
import zel.game.Game
import zel.net.clients.ClientConnections
import zel.net.crypto.RsaKey
import zel.protocol.typed.PacketPackage
import javax.sql.DataSource

class HandshakeModule(
    parts: Parts,
    game: Game,
) {
    class Parts(
        val config: Configuration,
        val rsaKey: RsaKey?,
        val users: DataSource,
        val threading: GameThreads,
        val clients: ClientConnections,
        val loginState: LoginState,
        val texts: Texts,
    )

    class LoginState(
        val gameReady: () -> Boolean,
        val loadAllowsLogin: () -> Boolean,
        val now: () -> Int,
    )

    private val login =
        Login(
            LoginAdmission(
                parts.config,
                parts.rsaKey,
                parts.loginState.gameReady,
                parts.loginState.loadAllowsLogin,
            ),
            PlayerSessions(parts.clients, requireNotNull(game.players), game.maintenance, parts.threading),
            LoginBurst(parts.config, game.rewardTrackService, parts.loginState.now) { game.rooms.summary(it) != null },
            LoginFollowUps(
                parts.config,
                LoginFollowUps.Services(
                    parts.threading,
                    requireNotNull(game.sanctions),
                    requireNotNull(game.moderationService),
                    requireNotNull(game.achievements),
                    LoginAchievements(requireNotNull(game.achievements)),
                    requireNotNull(game.calendars),
                    requireNotNull(game.catalog),
                ),
                Sql(parts.users),
                parts.loginState.now,
            ),
            parts.clients,
            parts.texts,
        )

    val packages: List<PacketPackage> =
        listOf(
            HandshakePackets(
                parts.clients,
                login,
                MachineIdBinding(parts.clients, requireNotNull(game.moderationService), parts.threading),
            ),
        )
}
