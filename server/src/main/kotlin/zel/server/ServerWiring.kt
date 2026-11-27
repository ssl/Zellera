package zel.server

import zel.core.BuildInfo
import zel.core.Configuration
import zel.core.ServerState
import zel.core.Texts
import zel.core.load.LoadWatch
import zel.core.rules.rules
import zel.core.threading.GameThreads
import zel.db.Database
import zel.ext.GameExtensions
import zel.game.Game
import zel.game.achievements.AchievementProgress
import zel.game.achievements.countFor
import zel.game.bots.Bot
import zel.game.bots.BotEnvironment
import zel.game.camera.CameraModule
import zel.game.catalog.CatalogModule
import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.landing.LandingModule
import zel.game.navigation.NavigatorModule
import zel.game.pets.Pet
import zel.game.players.Player
import zel.game.safetyquiz.RulesQuiz
import zel.game.treasurehunt.TreasureHunts
import zel.net.clients.ClientConnections
import zel.net.crypto.RsaKey
import zel.protocol.ambassadors.AmbassadorsPackets
import zel.protocol.bots.BotPackets
import zel.protocol.camera.CameraPackets
import zel.protocol.crafting.CraftingPackets
import zel.protocol.discord.DiscordPackets
import zel.protocol.earnings.EarningsPackets
import zel.protocol.featuredlooks.FeaturedLooksPackets
import zel.protocol.friends.FriendsPackets
import zel.protocol.handshake.HandshakeModule
import zel.protocol.helper.HelperPackets
import zel.protocol.icons.IconPackets
import zel.protocol.incoming.guilds.GuildModule
import zel.protocol.incoming.moderation.ModerationPacketsModule
import zel.protocol.incoming.users.PlayersModule
import zel.protocol.misc.MiscModule
import zel.protocol.polls.PollPackets
import zel.protocol.safetyquiz.SafetyQuizPackets
import zel.protocol.selfdonation.SelfDonationPackets
import zel.protocol.treasurehunt.TreasureHuntPackets
import zel.protocol.typed.PacketPackage
import zel.room.RoomEngine
import zel.room.RoomEngineServices
import zel.room.RoomEngineWiring
import zel.room.chat.GameChat
import zel.room.unit.bot.UnitHost
import java.sql.Connection
import java.sql.ResultSet
import java.time.Duration

@Suppress("LongParameterList") // the services every module is built from
class ServerWiring(
    config: Configuration,
    rsaKey: RsaKey?,
    database: Database,
    threading: GameThreads,
    texts: Texts,
    game: Game,
    clients: ClientConnections,
    load: LoadWatch,
    state: ServerState,
) {
    val services: GameServices = game.gameServices

    private val loginState =
        HandshakeModule.LoginState(
            gameReady = { state.isReady },
            loadAllowsLogin = load::admitsLogin,
            now = services::unixTimestamp,
        )

    val handshake =
        HandshakeModule(
            HandshakeModule.Parts(config, rsaKey, database.dataSource, threading, clients, loginState, texts),
            game,
        )

    val misc = MiscModule(requireNotNull(game.resolutions), loginState.now) { game.achievements }

    val guilds =
        GuildModule(
            requireNotNull(game.guilds),
            requireNotNull(game.players),
            game.roomListings,
            requireNotNull(game.wordFilter),
            config,
            texts,
            threading,
            { services.badgeImager },
            loginState.now,
            live = { game.rooms },
        )

    val progression: List<PacketPackage> = game.progressionPackets()

    val social: List<PacketPackage> =
        requireNotNull(game.messengerModule).let { messenger ->
            listOf(
                FriendsPackets(
                    messenger,
                    requireNotNull(game.players),
                    game.roomListings,
                    requireNotNull(game.wordFilter),
                    live = { game.rooms },
                ),
                HelperPackets(
                    config,
                    requireNotNull(game.sanctions),
                    requireNotNull(game.moderationService),
                    messenger.clock,
                ),
                AmbassadorsPackets(),
            )
        }

    val catalog =
        CatalogModule(
            requireNotNull(game.catalogServices) { "the catalog is not loaded" },
        )

    val moderation = ModerationPacketsModule(game, texts, config, database.dataSource, loginState.now)

    val bots = BotPackets()
    val players = PlayersModule(services)

    val camera =
        CameraModule(config, database.dataSource, loginState.now).let {
            CameraPackets(services, it.service, it.competition)
        }
    val crafting = CraftingPackets(services)
    val discord = DiscordPackets()
    val earnings = EarningsPackets(requireNotNull(game.incomeRewards))
    val icons = IconPackets { game.rooms }
    val featuredLooks = FeaturedLooksPackets(requireNotNull(game.featuredLooks))
    val polls = PollPackets(services, requireNotNull(game.polls), requireNotNull(game.pollOffers)) { game.rooms }
    val safetyQuiz = SafetyQuizPackets(RulesQuiz()) { game.achievements ?: AchievementProgress.NONE }
    val selfDonation = SelfDonationPackets(services)
    val treasureHunt = TreasureHuntPackets({ game.treasureHunts }) { config.getBoolean(TreasureHunts.SETTING) }

    /** Loaded before the rooms; they start once the room engine stands. */
    val extensions: GameExtensions = GameExtensions.load(config, BuildInfo.VERSION)

    val roomEngine: RoomEngine =
        RoomEngineWiring(
            RoomEngineServices(
                config,
                database.dataSource,
                clients,
                definitions = { id -> game.items?.getItem(id) },
                persist = threading::write,
                persisted = { threading.awaitWrites(Duration.ofSeconds(ROOM_WRITES_SECONDS)) },
                persistInOrder = { key, job -> threading.write(key, job) },
                inventoryItem = { id -> game.items?.loadFurni(id) },
                progress = { id, event, amount -> queueProgress(threading, game, id, event, amount) },
                achievements = { game.achievements ?: AchievementProgress.NONE },
                items = { game.items?.services },
                catalog = { game.catalog },
                listings = game.roomListings,
                navigator = { game.navigator },
                youtube = { game.items?.youtube },
                wordFilter = { game.wordFilter },
                chat =
                    GameChat(
                        clients,
                        database.dataSource,
                        threading::write,
                        styles = { game.chatStyles },
                        wordFilter = { game.wordFilter },
                        commands = { game.commands },
                        config,
                    ) { rules },
                units = unitHost(config, texts, game, threading),
                guildFields = { id ->
                    game.guilds?.let { guilds ->
                        guilds.byId(id)?.let { guild ->
                            listOf(
                                guild.id.toString(),
                                guild.badge,
                                guilds.parts.symbolColourHex(guild),
                                guilds.parts.backgroundColourHex(guild),
                            )
                        }
                    }
                },
                leaderboard = { game.badgeCaches.leaderboard },
                badgeExists = { code -> game.knownBadges.exists(code) },
                accountLevels = { game.accountLevels },
                wiredEarnings = { game.wiredCreditEarnings },
                definitionByCode = { code -> game.items?.getItem(code) },
                permissions = { game.permissions },
                extensions = extensions,
            ),
        ).build().also { engine -> game.rooms = engine.rooms }

    val landing =
        LandingModule(
            requireNotNull(game.landing),
            config,
            loginState.now,
        )

    val navigator = NavigatorModule(game, config, database)

    val snowball: List<PacketPackage> = game.snowball?.packages().orEmpty()

    companion object {
        private const val VISIT_LIMIT = 10
        private const val ROOM_WRITES_SECONDS = 30L
        private const val ANY_SADDLE = "horse_saddle"

        /** On the writer, so a room's lock never waits on the commit. */
        private fun queueProgress(
            threading: GameThreads,
            game: Game,
            playerId: Int,
            event: String,
            amount: Int,
        ) = threading.write(AchievementProgress.queueKey(playerId)) {
            game.achievements?.countFor(playerId, event, amount)
        }

        fun unitHost(
            config: Configuration,
            texts: Texts,
            game: Game,
            threading: GameThreads,
        ): UnitHost =
            object : UnitHost {
                override fun player(id: Int): Player? = game.players?.find(id)

                override fun bot(row: ResultSet): Bot? = if (game.bots == null) null else Bot.read(row)

                override fun butlerServes() = game.bots?.butlerWords.orEmpty()

                override fun roomVisits(
                    roomId: Int,
                    since: Int,
                    excludeName: String,
                ) = game.moderationService
                    ?.visits
                    ?.recentVisitors(roomId, since, excludeName, VISIT_LIMIT)
                    .orEmpty()
                    .map { UnitHost.Visit(it.name, it.enteredAt) }

                override fun textOf(
                    key: String,
                    default: String,
                ) = config.getValue(key, default)

                override fun pet(row: ResultSet): Pet? = game.pets?.maker?.fromRow(row)

                override fun playerName(id: Int): String =
                    game.players
                        ?.profileOf(id)
                        ?.name
                        .orEmpty()

                override fun text(
                    key: String,
                    default: String,
                ): String = texts.textOr(key, default)

                override fun filter(
                    player: Player,
                    text: String,
                ): String = game.wordFilter?.filter(text, player) ?: text

                override fun censor(text: String): String = game.wordFilter?.filter(text, null) ?: text

                override fun config(
                    key: String,
                    default: Int,
                ): Int = config.getInt(key, default)

                override fun saddle(definitionId: Int): Item? =
                    game.items?.let { items ->
                        items.getItem(definitionId)
                            ?: items.getItems().values.firstOrNull { it.name.startsWith(ANY_SADDLE) }
                    }

                override fun newFurni(
                    connection: Connection,
                    playerId: Int,
                    definition: Item,
                ): OwnedFurni? = game.items?.newFurni(connection, playerId, definition, 0, 0, "")

                override fun progress(
                    playerId: Int,
                    achievement: String,
                    amount: Int,
                ) = queueProgress(threading, game, playerId, achievement, amount)
            }

        fun botEnvironment(services: GameServices): BotEnvironment =
            BotEnvironment { services.database.dataSource.connection }
    }
}
