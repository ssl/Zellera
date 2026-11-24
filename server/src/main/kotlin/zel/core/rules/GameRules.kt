package zel.core.rules

import zel.content.ClientStrings
import zel.util.HOURS_PER_DAY
import zel.util.MINUTES_PER_HOUR
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GameRules private constructor(
    read: Reader,
) {
    private val introductionTrack = read.flag("reward_track.introduction.enabled")
    private val halloweenTrack = read.flag("reward_track.halloween.enabled")
    val premiumForEveryone = read.flag("reward_track.premium_for_everyone")
    val premiumPass = PremiumPass(read)

    val dailyTasks = read.flag("daily_tasks.enabled")
    val dailyGift = read.flag("daily_gift.enabled")
    val dailyRespects = read.number("respect.daily_players")
    val dailyPetRespects = read.number("respect.daily_pets")
    val respectRefillCost = read.number("respect.replenish_cost_points")

    init {
        // The web applies these at registration; read here only so a bad value fails the start.
        read.number("new_player.credits")
        read.number("new_player.points")
        read.number("new_player.premium")
        read.text("new_player.look")
        read.flag("build_pass.membership_for_everyone")
    }

    val membershipDays = read.number("build_pass.membership_days")
    val memberFurniLimit = read.number("build_pass.member_furni_limit")
    val monthFurniBonus = read.number("build_pass.month_furni_bonus")
    val nonMemberFurniLimit = read.number("build_pass.non_member_furni_limit")
    val nonMemberMaxFurniLimit = read.number("build_pass.non_member_max_furni_limit")
    val nonMemberAloneOnly = read.flag("build_pass.non_member_alone_only")
    val roomLock = read.flag("build_pass.room_lock")
    val placementWarning = read.flag("build_pass.placement_warning")

    val floorPlanEditor = read.choice<FloorPlanAccess>("floor_plan.editor")
    val floorPlanRequiresFloor = read.flag("floor_plan.requires_floor")
    val floorPlanMaxArea = read.number("floor_plan.max_area")
    val floorPlanMaxOpenTiles = read.number("floor_plan.max_open_tiles")

    val snowball = read.flag("snowball.enabled")
    val snowballFreeGames = read.choice<FreeGames>("snowball.free_games")
    val snowballDailyFreeGames = read.number("snowball.daily_free_games")
    val snowballDailyFreeGamesClub = read.number("snowball.daily_free_games_club")

    val snowballTokenOffers: List<GameTokenOffer> =
        TOKEN_OFFER_GAMES
            .map { games ->
                GameTokenOffer(
                    games,
                    read.number("snowball.tokens_$games.credits"),
                    read.number("snowball.tokens_$games.premium"),
                )
            }.filter { it.credits > 0 || it.premium > 0 }

    val autoSleep = read.flag("idle.auto_sleep")
    val sleepSeconds = read.number("idle.sleep_seconds")
    val roomSleepSeconds = read.number("idle.room_sleep_min_seconds")..read.number("idle.room_sleep_max_seconds")
    val idleKick = read.flag("idle.kick_enabled")
    val newRoomKickSeconds = read.number("idle.new_room_kick_seconds")
    val roomKickSeconds = read.number("idle.room_kick_min_seconds")..read.number("idle.room_kick_max_seconds")

    val roomMaxFurni = read.number("room.max_furni")
    val roomMaxNotes = read.number("room.max_notes")
    val roomMaxPets = read.number("room.max_pets")
    val roomMaxBots = read.number("room.max_bots")
    val roomSpectatorSlots = read.number("room.spectator_slots")
    val roomEnforceCategoryVisitors = read.number("room.enforce_category_visitors")
    val roomMaxFilterWords = read.number("room.max_filter_words")
    val floodMuteSeconds = read.number("chat.flood_mute_seconds")
    val wordFilterReplacement = read.text("word_filter.replacement")

    val rollerDefaultSpeed = read.number("rollers.default_speed")
    val rollerHeightTolerance = read.number("rollers.height_tolerance_hundredths") / HUNDREDTHS

    val offlineMessagesMax = read.number("messenger.offline_messages_max")

    val pets = PetRules(read)

    val maxItemsPerOrder = read.number("catalog.max_items_per_order")

    val maxRooms = read.number("player.max_rooms")
    val maxRoomsClub = read.number("player.max_rooms_club")
    val maxFriends = read.number("player.max_friends")
    val maxFriendsClub = read.number("player.max_friends_club")
    val maxFavouriteRooms = read.number("player.max_favourite_rooms")
    val enterHomeRoom = read.flag("login.enter_home_room")
    val loginDefaultRoom = read.number("login.default_room")
    val playerNameMin = read.number("player.name.min_length")
    val playerNameMax = read.number("player.name.max_length")
    val playerNamePunctuation = read.text("player.name.punctuation")

    val accountLevelHostingDivisor = read.number("account_level.hosting_divisor")
    val resolutionDays = read.number("achievements.resolution_days")

    val trading = read.flag("trading.enabled")
    val tradeCreditFurni = read.choice<TradeCreditFurni>("trading.credit_furni")
    val tradeBatchCap = read.number("trading.batch_cap")
    val marketplace = read.flag("marketplace.enabled")
    val marketplaceTokensRequired = read.flag("marketplace.tokens_required")
    val marketplaceTokenBatchPrice = read.number("marketplace.token_batch_price")
    val marketplaceTokenBatchSize = read.number("marketplace.token_batch_size")
    val recycler = read.flag("recycler.enabled")
    val groupsRequireClub = read.flag("groups.require_club")
    val groupMaxMembers = read.number("groups.max_members")
    val groupMaxMemberships = read.number("groups.max_memberships")
    val groupMaxMembershipsClub = read.number("groups.max_memberships_club")
    val clubPayday = read.flag("club.payday_enabled")
    val monthlyGiftNotice = read.flag("club.monthly_gift_notice")

    val openingHours = read.flag("opening_hours.enabled")
    val opensAtMinute = read.clockTime("opening_hours.opens_at")
    val closesAtMinute = read.clockTime("opening_hours.closes_at")
    val seasonalCurrency = read.flag("seasonal_currency.enabled")
    val seasonalCurrencyTypes = read.numbers("seasonal_currency.types")

    val rates = MessageRates(read)
    val limits = EngineLimits(read)

    fun trackEnabled(
        trackId: String,
        halloweenTrackId: String? = ClientStrings[ClientStrings.HALLOWEEN_TRACK],
    ): Boolean =
        when (trackId) {
            INTRODUCTION_TRACK -> introductionTrack
            halloweenTrackId -> halloweenTrack
            else -> true
        }

    class PremiumPass internal constructor(
        read: Reader,
    ) {
        val credits = read.number("reward_track.premium_pass.credits")
        val premium = read.number("reward_track.premium_pass.premium_price")
        val boostPercent = read.number("reward_track.premium_pass.boost_percent")
        val instantPoints = read.number("reward_track.premium_pass.instant_points")

        val onSale: Boolean get() = credits > 0 || premium > 0

        val pointsFactor: Double get() = 1 + boostPercent / HUNDREDTHS
    }

    /** Milliseconds between two accepted messages of one kind from one player. */
    class MessageRates internal constructor(
        read: Reader,
    ) {
        val buildPassPlaceGapMillis = read.number("rates.build_pass_place_gap_millis").toLong()
        val furniMoveGapMillis = read.number("rates.furni_move_gap_millis").toLong()
        val shareSyncGapMillis = read.number("rates.share_sync_gap_millis").toLong()
        val wiredGiveFurniGapMillis = read.number("rates.wired_give_furni_gap_millis").toLong()
    }

    class EngineLimits internal constructor(
        read: Reader,
    ) {
        val wiredNesting = read.number("limits.wired_nesting")
        val wiredStepsPerFiring = read.number("limits.wired_steps_per_firing")
        val wiredSignalsPerFiring = read.number("limits.wired_signals_per_firing")
        val wiredSignalHops = read.number("limits.wired_signal_hops")
        val wiredSignalForwarded = read.number("limits.wired_signal_forwarded")
        val wiredCallTiles = read.number("limits.wired_call_tiles")
        val wiredCallStacks = read.number("limits.wired_call_stacks")
        val frameBytes = read.number("limits.frame_bytes")
        val messagesPerKindPerSecond = read.number("limits.messages_per_kind_per_second")
        val messagesPerSecond = read.number("limits.messages_per_second")
        val moderationChatlogLines = read.number("limits.moderation_chatlog_lines")
        val moderationRoomVisits = read.number("limits.moderation_room_visits")
        val reportsOpenPerPlayer = read.number("limits.reports_open_per_player")
    }

    class PetRules internal constructor(
        read: Reader,
    ) {
        val restEnergyDivisor = read.number("pets.needs.rest_energy_divisor")
        val thirstSeconds = read.number("pets.needs.thirst_seconds")
        val walkingSadnessSeconds = read.number("pets.needs.walking_sadness_seconds")
        val moodEverySeconds = read.number("pets.mood.every_seconds")
        val moodGestureSeconds = read.number("pets.mood.gesture_seconds")
        val wearyEnergyDivisor = read.number("pets.mood.weary_energy_divisor").coerceAtLeast(1)
        val thirstyFrom = read.number("pets.mood.thirsty_from")
        val happyFromHappiness = read.number("pets.mood.happy_from_happiness")
        val sadUpToHappiness = read.number("pets.mood.sad_up_to_happiness")
        val obeyRestedPercent = read.number("pets.obedience.rested_percent")
        val obeyDrainedPercent = read.number("pets.obedience.drained_percent")
        val drainedEnergyDivisor = read.number("pets.obedience.drained_energy_divisor").coerceAtLeast(1)
        val sulksBelowHappiness = read.number("pets.obedience.sulks_below_happiness")
        val commandGapSeconds = read.number("pets.obedience.gap_seconds")
        val repeatWindowSeconds = read.number("pets.obedience.repeat_window_seconds")
        val repeatsAllowed = read.number("pets.obedience.repeats_allowed")
        val grumbleOneIn = read.number("pets.obedience.grumble_one_in").coerceAtLeast(1)
        val staySeconds = read.number("pets.obedience.stay_seconds")
        val rarePlantParentFrom = read.number("pets.plants.rare_parent_from")
        val onePlantSeedPercent = read.number("pets.plants.one_seed_percent")
        val breedingSpread = read.number("pets.breeding.spread_hundredths") / HUNDREDTHS
        val breedingNarrowing = read.number("pets.breeding.narrowing_hundredths") / HUNDREDTHS
        val breedingTierCeilings =
            (1..BREEDING_TIERS).map { read.number("pets.breeding.tier_${it}_up_to_level").toDouble() }

        private companion object {
            const val BREEDING_TIERS = 4
            const val HUNDREDTHS = 100.0
        }
    }

    internal class Reader(
        private val values: Map<String, String>,
        private val problems: MutableList<String>,
    ) {
        val used = HashSet<String>()

        private fun value(key: String): String? {
            used += key
            return values[key].also { if (it == null) problems += "$key is missing" }
        }

        fun flag(key: String): Boolean =
            when (val text = value(key)) {
                null, "true" -> true
                "false" -> false
                else -> false.also { problems += "$key must be true or false, not '$text'" }
            }

        fun number(key: String): Int {
            val text = value(key) ?: return 0
            return wholeNumber(text) ?: 0.also {
                problems += "$key must be a whole number of 0 or more, not '$text'"
            }
        }

        fun text(key: String): String {
            val text = value(key) ?: return ""
            return text.ifEmpty { text.also { problems += "$key must not be empty" } }
        }

        /** `HH:MM` as the minute of the day. */
        fun clockTime(key: String): Int {
            val text = value(key) ?: return 0
            val bad = { 0.also { problems += "$key must be a time of day as HH:MM, not '$text'" } }
            val (hour, minute) = CLOCK_TIME.matchEntire(text)?.destructured ?: return bad()
            if (hour.toInt() >= HOURS_PER_DAY || minute.toInt() >= MINUTES_PER_HOUR) return bad()
            return hour.toInt() * MINUTES_PER_HOUR + minute.toInt()
        }

        /** Plain digits only, as the web side reads them. */
        private fun wholeNumber(text: String): Int? = text.takeIf { DIGITS.matches(it) }?.toIntOrNull()

        /** Comma-separated whole numbers of 0 or more, at least one. */
        fun numbers(key: String): List<Int> {
            val text = value(key) ?: return emptyList()
            val parts = text.split(',').map { it.trim() }
            val numbers = parts.mapNotNull(::wholeNumber)
            if (numbers.isEmpty() || numbers.size != parts.size) {
                problems += "$key must list whole numbers of 0 or more, separated by commas, not '$text'"
            }
            return numbers
        }

        inline fun <reified T : Enum<T>> choice(key: String): T {
            val options = enumValues<T>()
            val text = value(key) ?: return options.first()
            return options.firstOrNull { it.name.lowercase() == text } ?: options.first().also {
                problems += "$key must be one of ${options.joinToString { o -> o.name.lowercase() }}, not '$text'"
            }
        }
    }

    companion object {
        const val FILE_VARIABLE = "ZEL_GAME_RULES_FILE"
        private const val RELATIVE_FILE = "config/game-rules.conf"
        private const val MAX_PLAYER_NAME = 32
        private const val INTRODUCTION_TRACK = "introduction"
        private const val HUNDREDTHS = 100.0
        private val CLOCK_TIME = Regex("(\\d{2}):(\\d{2})")
        private val DIGITS = Regex("\\d+")
        private val TOKEN_OFFER_GAMES = listOf(10, 100, 300)

        /** [local] holds the operator's own values, each over the same key of [text]. */
        fun parse(
            text: String,
            source: String = RELATIVE_FILE,
            local: String? = null,
        ): GameRules {
            val problems = ArrayList<String>()
            val values = read(text, "", problems)
            local?.let { values.putAll(read(it, "the local file: ", problems)) }
            val reader = Reader(values, problems)
            val rules = GameRules(reader)
            (values.keys - reader.used).forEach { problems += "$it is not a known rule" }
            if (rules.playerNameMin !in 1..rules.playerNameMax || rules.playerNameMax > MAX_PLAYER_NAME) {
                problems += "player.name.min_length and player.name.max_length must be 1 or more, in order, " +
                    "at most $MAX_PLAYER_NAME"
            }
            if (problems.isNotEmpty()) {
                throw GameRulesException("Game rules file $source: ${problems.joinToString("; ")}")
            }
            return rules
        }

        fun load(variable: String? = System.getenv(FILE_VARIABLE)): GameRules {
            val path = variable?.takeIf { it.isNotBlank() }?.let { Paths.get(it) } ?: findUpwards()
            if (path == null || !Files.isRegularFile(path)) {
                throw GameRulesException(
                    "Game rules file ${path ?: RELATIVE_FILE} is missing; set $FILE_VARIABLE or check it out",
                )
            }
            val local = localFile(path).takeIf { Files.isRegularFile(it) }?.let { Files.readString(it) }
            return parse(Files.readString(path), path.toString(), local)
        }

        /** `game-rules.local.conf` next to `game-rules.conf`. */
        fun localFile(file: Path): Path =
            file.resolveSibling(file.fileName.toString().removeSuffix(".conf") + ".local.conf")

        private fun read(
            text: String,
            label: String,
            problems: MutableList<String>,
        ): LinkedHashMap<String, String> {
            val values = LinkedHashMap<String, String>()
            text.lineSequence().forEachIndexed { index, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                val split = line.indexOf('=')
                if (split < 1) {
                    problems += "${label}line ${index + 1} is not key=value: '$line'"
                } else if (values.put(line.take(split).trim(), line.substring(split + 1).trim()) != null) {
                    problems += "$label${line.take(split).trim()} is set twice"
                }
            }
            return values
        }

        private fun findUpwards(): Path? =
            generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
                .map { it.resolve(RELATIVE_FILE) }
                .firstOrNull { Files.isRegularFile(it) }
    }
}
