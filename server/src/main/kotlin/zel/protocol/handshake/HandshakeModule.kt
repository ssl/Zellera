package zel.protocol.handshake

import zel.core.Configuration
import zel.core.Texts
import zel.core.threading.GameThreads
import zel.db.Sql
import zel.game.Game
import zel.net.clients.ClientConnections
import zel.net.crypto.RsaKey
import zel.protocol.typed.PacketPackage
import zel.security.ClientCheck
import zel.security.SuspicionLog
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
        val suspicions: SuspicionLog = SuspicionLog.None,
    )

    class LoginState(
        val gameReady: () -> Boolean,
        val loadAllowsLogin: () -> Boolean,
        val now: () -> Int,
    )

    private val clientCheck =
        ClientCheck(parts.config, parts.suspicions, { task, afterMillis -> parts.threading.run(task, afterMillis) })

    private val login =
        Login(
            LoginAdmission(
                parts.config,
                parts.rsaKey,
                parts.loginState.gameReady,
                parts.loginState.loadAllowsLogin,
            ),
            PlayerSessions(
                parts.clients,
                requireNotNull(game.players),
                game.openingHoursAccess,
                parts.threading,
                crowd = LoginCrowd(parts.config),
                suspicions = parts.suspicions,
            ),
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
            parts.suspicions,
            clientCheck,
        )

    val packages: List<PacketPackage> =
        listOf(
            HandshakePackets(
                parts.clients,
                login,
                MachineIdBinding(parts.clients, requireNotNull(game.moderationService), parts.threading),
                PacketShuffling(parts.config, parts.rsaKey?.modulusHex, parts.suspicions),
                clientCheck,
            ),
        )
}
