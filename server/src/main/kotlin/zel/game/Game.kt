package zel.game

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.Lifecycle
import zel.core.Configuration
import zel.core.DailyTaskPulseUpdater
import zel.core.Recurring
import zel.core.config.DisplayNameStore
import zel.core.config.DisplayNames
import zel.core.console.ShutdownSchedule
import zel.core.rules.rules
import zel.core.threading.GameThreads
import zel.game.achievements.Achievements
import zel.game.achievements.AchievementsModule
import zel.game.achievements.resolution.Resolutions
import zel.game.bots.Bots
import zel.game.camera.CameraAssetStore
import zel.game.campaign.calendar.CalendarModule
import zel.game.campaign.calendar.CampaignCalendars
import zel.game.catalog.Catalog
import zel.game.catalog.CatalogServices
import zel.game.catalog.OfferPreviewImageStore
import zel.game.catalog.OfferPreviewImages
import zel.game.claims.ClaimsModule
import zel.game.commands.CommandsModule
import zel.game.commands.SpokenCommands
import zel.game.communitygoals.CommunityGoalsModule
import zel.game.crafting.Crafting
import zel.game.earnings.EarningsModule
import zel.game.earnings.IncomeRewards
import zel.game.earnings.WiredCreditEarnings
import zel.game.economy.Currencies
import zel.game.economy.CurrencyStore
import zel.game.economy.EconomyLedger
import zel.game.effects.EffectRoleStore
import zel.game.effects.EffectRoles
import zel.game.featuredlooks.FeaturedLooks
import zel.game.games.GameAchievementCounters
import zel.game.games.snowball.SnowballModule
import zel.game.guilds.GuildDirectory
import zel.game.icons.IconService
import zel.game.items.FurniTitles
import zel.game.items.ItemDataUrls
import zel.game.items.ItemServices
import zel.game.items.Items
import zel.game.landing.Landing
import zel.game.messenger.MessengerModule
import zel.game.moderation.ModerationModule
import zel.game.moderation.ModerationService
import zel.game.moderation.Sanctions
import zel.game.moderation.WordFilter
import zel.game.navigation.Navigator
import zel.game.permissions.Permissions
import zel.game.permissions.PermissionsModule
import zel.game.pets.BreedingCurve
import zel.game.pets.PetServices
import zel.game.pets.PetsModule
import zel.game.players.ChatStyles
import zel.game.players.Players
import zel.game.players.inventory.BadgeCaches
import zel.game.players.inventory.BadgeTexts
import zel.game.players.inventory.KnownBadges
import zel.game.players.level.AccountLevels
import zel.game.players.level.JdbcAccountLevelStore
import zel.game.players.subscriptions.MembershipWatch
import zel.game.players.subscriptions.Subscriptions
import zel.game.polls.PollModule
import zel.game.polls.PollOffers
import zel.game.polls.Polls
import zel.game.quests.Quests
import zel.game.quests.QuestsModule
import zel.game.rewardtracks.RewardTrackService
import zel.game.rewardtracks.RewardTracksModule
import zel.game.treasurehunt.TreasureHuntModule
import zel.game.treasurehunt.TreasureHunts
import zel.game.webcommands.WebCommandProcessor
import zel.game.webcommands.WebCommandsModule
import zel.protocol.outgoing.guilds.GuildMessages
import zel.protocol.outgoing.moderation.ModerationOutbox
import zel.protocol.typed.PacketPackage
import zel.room.chat.ChatBubbleStore
import zel.room.chat.ChatBubbleStyles
import zel.room.listing.RoomListings
import zel.room.port.RoomPort
import zel.server.GameServices
import zel.server.RunningGameServices
import zel.server.ServerWiring
import zel.util.GameZone
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.ThreadLocalRandom

@Suppress("TooManyFunctions", "LargeClass") // the composition root of the game services
class Game(
    private val persistenceExecutor: Executor,
    private val iconService: IconService,
    private val threading: GameThreads,
    private val configuration: Configuration,
    private val core: GameCore,
) {
    private val parts = Lifecycle()
    private val now: () -> Int get() = core.now
    private val progressionPackets = mutableListOf<PacketPackage>()

    var economyLedger: EconomyLedger? = null
        private set
    var incomeRewards: IncomeRewards? = null
        private set
    var wiredCreditEarnings: WiredCreditEarnings? = null
        private set
    var players: Players? = null
        private set
    val badgeCaches: BadgeCaches by lazy { BadgeCaches(core.database) }

    val knownBadges: KnownBadges by lazy {
        val texts = BadgeTexts.installed()
        KnownBadges(
            { achievements?.all?.values.orEmpty() },
            { permissions?.ranks?.staffBadges.orEmpty() },
            texts::names,
        )
    }
    val accountLevels: AccountLevels by lazy {
        AccountLevels(
            JdbcAccountLevelStore(core.database.dataSource),
            { incomeRewards },
            { players?.find(it) },
            { task, delayMs -> threading.run(task, delayMs) },
            { rules.accountLevelHostingDivisor },
        )
    }
    val chatStyles: ChatStyles by lazy { ChatStyles(core.database) }
    val chatBubbles = ChatBubbleStyles()
    var navigator: Navigator? = null
        private set
    var guilds: GuildDirectory? = null
        private set

    val shutdownSchedule = ShutdownSchedule()
    var items: Items? = null
        private set
    var furniTitles: FurniTitles? = null
        private set
    var catalog: Catalog? = null
        private set

    var catalogServices: CatalogServices? = null
        private set
    var landing: Landing? = null
        private set
    var treasureHunts: TreasureHunts? = null
        private set

    var rooms: RoomPort
        get() = checkNotNull(builtRooms()) { "the room engine is not built yet" }
        set(value) {
            port = value
        }

    private var port: RoomPort? = null

    fun builtRooms(): RoomPort? = port

    val roomListings: RoomListings by lazy {
        RoomListings(
            core.database.dataSource,
            { builtRooms() },
            now,
            { configuration.getBoolean("navigator.popular.includes_public_rooms") },
        )
    }

    var commands: SpokenCommands? = null
        private set
    var permissions: Permissions? = null
        private set
    var bots: Bots? = null
        private set
    var moderationService: ModerationService? = null
        private set
    var sanctions: Sanctions? = null
        private set
    var pets: PetsModule? = null
        private set
    var achievements: Achievements? = null
        private set

    var messengerModule: MessengerModule? = null
        private set
    var wordFilter: WordFilter? = null
        private set
    var crafting: Crafting? = null
        private set
    var polls: Polls? = null
        private set
    var pollOffers: PollOffers? = null
        private set
    var subscriptions: Subscriptions? = null
        private set
    var calendars: CampaignCalendars? = null
        private set
    var featuredLooks: FeaturedLooks? = null
        private set
    var quests: Quests? = null
        private set

    var rewardTrackService: RewardTrackService? = null
        private set
    var resolutions: Resolutions? = null
        private set
    private var achievementsModule: AchievementsModule? = null

    var gameCounters: GameAchievementCounters? = null
        private set

    var snowball: SnowballModule? = null
        private set
    private var webCommandProcessor: WebCommandProcessor? = null

    val gameServices: GameServices by lazy {
        RunningGameServices(core, configuration, threading, this)
    }

    val maintenance: Maintenance by lazy {
        Maintenance(configuration)
    }

    fun load() {
        logger.info("loading the game")
        val pool = threading

        loadCoreServices()
        loadRoomsAndCommands()
        loadContentServices(pool)
        loadQuestsAndPublicRooms(pool)

        subscriptions =
            parts.start("subscription") { Subscriptions(gameServices) }

        val memberships = MembershipWatch(gameServices)
        recurring(pool, "subscription scheduler", memberships::period, memberships::check)
        val doors = OpeningHoursWatch(OpeningHours(), maintenance, { core.clients() })
        recurring(pool, "opening hours", { OpeningHoursWatch.EVERY }, doors::look)

        webCommandProcessor =
            parts.start("web commands", WebCommandProcessor::dispose) {
                WebCommandsModule(
                    core.database.dataSource,
                    requireNotNull(players),
                    requireNotNull(permissions),
                    requireNotNull(wordFilter),
                    configuration,
                    pool,
                    now,
                    { core.state.isReady },
                    badgeCaches,
                    knownBadges::exists,
                    accountLevels::inputChanged,
                    reloadTvPlaylists = {
                        configuration.reload()
                        Thread.ofVirtual().name("tv-playlists").start { this.items?.youtube?.load() }
                    },
                    kickBanned = { id -> moderationService?.banning?.kickBanned(id) == true },
                    releaseTicket = { id -> moderationService?.tickets?.releaseStuck(id) == true },
                ) { rooms }.processor
            }
        webCommandProcessor?.start()

        logger.info("game loaded")
    }

    private fun loadEconomy() {
        val dataSource = core.database.dataSource
        economyLedger = parts.start("economy ledger") { EconomyLedger(dataSource) }
        val earnings =
            parts.start("earnings") {
                EarningsModule(
                    dataSource,
                    GameZone.clock,
                    { players?.find(it) },
                    { items },
                )
            }
        incomeRewards = earnings.rewards
        wiredCreditEarnings = earnings.wiredCredits
    }

    private fun loadCoreServices() {
        loadEconomy()
        permissions =
            parts.start("permissions") {
                PermissionsModule(core.database.dataSource, now) {
                    configuration.getInt(Permissions.VOTE_MIN_RANK_KEY, Permissions.STANDARD_VOTE_MIN_RANK)
                }.permissions
            }
        players =
            parts.start("player", Players::dispose) { Players(gameServices, persistenceExecutor, iconService) }
        landing = parts.start("landing view") { Landing(gameServices) }
        val players = requireNotNull(this.players)
        loadCommunityGoals(players)
        val items = parts.start("item", Items::dispose, ::createItems)
        this.items = items
        items.load()
        loadTreasureHunt(items, players)
        furniTitles = parts.start("furni titles") { FurniTitles.from(configuration) }
        bots = parts.start("bot") { Bots(ServerWiring.botEnvironment(gameServices)) }
        pets =
            parts.start("pet") {
                gamePets(this, threading, configuration, core)
            }
        guilds =
            parts.start("guild", GuildDirectory::dispose) {
                GuildDirectory(
                    core.database.dataSource,
                    now,
                    GuildMessages(),
                    { id -> requireNotNull(players).find(id) },
                    { id -> requireNotNull(players).profileOf(id) },
                    { rooms },
                )
            }
        catalog = createCatalog()
    }

    private fun createCatalog(): Catalog {
        val catalogServices =
            gameCatalogServices(this, configuration, threading, core)
        this.catalogServices = catalogServices
        return parts.start("catalog", Catalog::dispose) { Catalog(catalogServices) }
    }

    private fun createItems(): Items {
        val pool = threading
        return Items(
            core.database.dataSource,
            pool,
            { catalog },
            { players },
            { furniTitles },
            gameItemServices(this, pool, configuration, core),
        )
    }

    private fun loadTreasureHunt(
        items: Items,
        players: Players,
    ) {
        val treasureHunt =
            parts.start("treasure hunt", { it.hunts.dispose() }) {
                TreasureHuntModule(
                    core.database,
                    items,
                    players,
                    now,
                )
            }
        treasureHunts = treasureHunt.hunts
    }

    private fun loadCommunityGoals(players: Players) {
        val communityGoals =
            parts.start("community goals") {
                CommunityGoalsModule(
                    core.database,
                    configuration,
                    players,
                    now,
                )
            }
        progressionPackets += communityGoals.packets
    }

    private fun spokenCommands(): SpokenCommands =
        parts.start("commands") {
            CommandsModule(
                core.texts,
                configuration,
                requireNotNull(players),
                { rooms },
                requireNotNull(permissions),
                { core.commandLog },
                core.info,
            ).spoken
        }

    private fun loadSnowball() {
        val game =
            SnowballModule(
                configuration,
                threading,
                core.database.dataSource,
                requireNotNull(players),
                requireNotNull(achievements),
                requireNotNull(wordFilter),
                requireNotNull(items),
                requireNotNull(incomeRewards),
                { requireNotNull(messengerModule).updates },
                { ItemDataUrls.site(configuration) },
            )
        snowball = game
        game.lobby.load()
    }

    private fun loadRoomsAndCommands() {
        gameCounters =
            GameAchievementCounters(
                { player, event, amount ->
                    achievements?.forEvent(event)?.forEach { it.progress(player, amount) }
                },
            )
        navigator =
            parts.start("navigator") { Navigator(gameServices, roomListings) }
    }

    private fun loadSmallFeatures() {
        val dataSource = core.database.dataSource
        val items = requireNotNull(this.items)
        crafting =
            parts.start("crafting", Crafting::dispose) { Crafting(dataSource, items) }
        val pollModule =
            parts.start("poll") {
                PollModule(dataSource, threading, now)
            }
        polls = pollModule.polls
        pollOffers = pollModule.offers
        featuredLooks =
            parts.start("featured looks") { FeaturedLooks(dataSource, configuration) }
    }

    private fun loadModeration() {
        val players = requireNotNull(this.players)
        val moderation =
            parts.start("moderation") {
                ModerationModule(
                    core.database.dataSource,
                    configuration,
                    core.texts,
                    players,
                    requireNotNull(permissions),
                    { rooms },
                    threading,
                    ModerationOutbox(players),
                    { checkNotNull(core.clients()) },
                    { requireNotNull(items).loadFurni(it) },
                    now,
                )
            }
        moderationService = moderation.service
        sanctions = moderation.sanctions
        wordFilter = moderation.wordFilter
    }

    private fun loadContentServices(pool: GameThreads) {
        loadModeration()
        commands = spokenCommands()
        val module =
            parts.start("achievement") {
                AchievementsModule(
                    core.database.dataSource,
                    requireNotNull(players),
                    { task, delayMs -> pool.run(task, delayMs) },
                    { id -> roomListings.category(id) },
                    now,
                    accountLevels,
                    badgeCaches,
                    chatStyles,
                )
            }
        module.install()
        achievementsModule = module
        achievements = module.achievements
        module.achievements.refresh()
        resolutions = module.resolutions
        progressionPackets += module.packets
        loadSocialModules(pool, module.achievements, requireNotNull(wordFilter))
        loadSmallFeatures()
        val calendar =
            parts.start("calendar", { it.calendars.dispose() }) {
                CalendarModule(
                    core.database,
                    requireNotNull(items),
                    ThreadLocalRandom::current,
                    now,
                )
            }
        calendars = calendar.calendars
        progressionPackets += calendar.packets
        parts.start("camera filters") { CameraAssetStore(core.database).load() }
        parts.start("display names") { DisplayNames.install(DisplayNameStore(core.database).readAll()) }
        parts.start("currencies") { Currencies.install(CurrencyStore(core.database).readAll()) }
        parts.start("effect roles") { EffectRoles.install(EffectRoleStore(core.database).readAll()) }
        parts.start(
            "offer preview images",
        ) { OfferPreviewImages.install(OfferPreviewImageStore(core.database).readAll()) }
        parts.start("room chat bubbles") {
            chatBubbles.addAll(ChatBubbleStore(core.database).readAll())
            chatStyles.reload()
        }
    }

    private fun loadSocialModules(
        pool: GameThreads,
        achievements: Achievements,
        words: WordFilter,
    ) {
        messengerModule =
            MessengerModule(
                core.database.dataSource,
                configuration,
                now,
                pool,
                { checkNotNull(core.clients()) },
                requireNotNull(players),
                achievements,
                words,
            )
    }

    private fun loadQuestsAndPublicRooms(pool: GameThreads) {
        val items = requireNotNull(this.items)
        val rewardTracks =
            parts.start("reward tracks") {
                RewardTracksModule(
                    core.database.dataSource,
                    persistenceExecutor,
                    items,
                    requireNotNull(pets),
                    requireNotNull(bots),
                    requireNotNull(catalog),
                    requireNotNull(achievementsModule).tally,
                )
            }
        rewardTrackService = rewardTracks.service
        progressionPackets += rewardTracks.packets
        val questModule =
            parts.start("quest") {
                QuestsModule(
                    core.database.dataSource,
                    configuration,
                    items,
                    requireNotNull(players),
                    rewardTracks.service,
                    requireNotNull(achievementsModule).hooks.explore,
                    { write -> pool.write(write) },
                    { key, write -> pool.write(key, write) },
                    now,
                ) { player, campaign ->
                    gameCounters?.questCompleted(player, campaign)
                }
            }
        quests = questModule.quests
        questModule.install()
        progressionPackets += questModule.packets
        DailyTaskPulseUpdater(questModule.dailyTasks, pool, core.state) { players }.start()
        rewardTracks.service.load()
        val claims =
            parts.start("product claims") {
                ClaimsModule(core.database, items, now)
            }
        progressionPackets += claims.packets
        loadSnowball()

        requireNotNull(navigator).load()
    }

    private fun recurring(
        pool: GameThreads,
        name: String,
        period: () -> Duration,
        work: () -> Unit,
    ) {
        parts.start(name, Recurring::end) { Recurring(pool, period, work = work).also(Recurring::start) }
    }

    fun progressionPackets(): List<PacketPackage> = progressionPackets.toList()

    fun dispose() {
        parts.stop()
        logger.info("game stopped")
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(Game::class.java)
    }
}

/** The catalog's view of the running server; the connection layer resolves when used. */
private fun gameCatalogServices(
    game: Game,
    configuration: Configuration,
    threading: GameThreads,
    core: GameCore,
) = CatalogServices(
    game,
    configuration,
    core.texts,
    threading,
    core.database,
    ThreadLocalRandom.current(),
    core.now,
    { game.shutdownSchedule.closesAt },
    core.clients,
)

/** The pets; achievements, players and items resolve when used, they load after the pets. */
private fun gamePets(
    game: Game,
    threading: GameThreads,
    configuration: Configuration,
    core: GameCore,
) = PetsModule(
    PetServices(
        threading,
        configuration,
        core.texts,
        core.database.dataSource,
        ThreadLocalRandom::current,
        core.now,
        { requireNotNull(game.achievements) },
        { requireNotNull(game.players) },
        { requireNotNull(game.items) },
    ),
    rules.pets.let { BreedingCurve(it.breedingSpread, it.breedingNarrowing, it.breedingTierCeilings) },
)

/** The furniture interactions' view of the server; the services built after the items resolve when used. */
private fun gameItemServices(
    game: Game,
    threading: GameThreads,
    configuration: Configuration,
    core: GameCore,
) = ItemServices(
    threading,
    configuration,
    core.database.dataSource,
    { game.items },
    { game.catalog },
    { game.pets },
)
