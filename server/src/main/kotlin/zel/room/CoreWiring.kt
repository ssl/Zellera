package zel.room

import zel.core.rules.rules
import zel.protocol.trading.TypedTradeMessages
import zel.room.chat.DanceLifeHooks
import zel.room.chat.DanceWalkHooks
import zel.room.chat.Dances
import zel.room.chat.HeadTurns
import zel.room.chat.JdbcRoomWordStore
import zel.room.chat.RoomWords
import zel.room.entry.ArrivalNotes
import zel.room.entry.EntryLifeHooks
import zel.room.entry.EntryReader
import zel.room.entry.GameEntryEnvironment
import zel.room.entry.JdbcEntryStore
import zel.room.entry.RoomLeaving
import zel.room.furni.FurniSaves
import zel.room.furni.place.PlayerFurniInventory
import zel.room.furni.place.StoredFurniAccess
import zel.room.furni.place.UnitsOnTiles
import zel.room.furni.roller.Rollers
import zel.room.furni.transit.TransitHolds
import zel.room.furni.transit.TransitStore
import zel.room.games.GameWalkHooks
import zel.room.games.GamesPart
import zel.room.life.LifeHooks
import zel.room.life.LifeTimes
import zel.room.life.LifeTiming
import zel.room.life.LifeWalkHooks
import zel.room.life.RoomLife
import zel.room.life.ServerLifeHooks
import zel.room.map.FloorStore
import zel.room.map.Point
import zel.room.persist.FurniStore
import zel.room.persist.PlayerCounts
import zel.room.persist.RoomStore
import zel.room.port.GroupRoomStore
import zel.room.quests.QuestWalkHooks
import zel.room.rights.GroupRights
import zel.room.rights.JdbcRightsStore
import zel.room.rights.RoomMutes
import zel.room.trade.RoomTrades
import zel.room.trade.TradeStore
import zel.room.unit.UnitKind
import zel.room.unit.path.PathSearch
import zel.room.unit.walk.Walking
import zel.util.MILLIS_PER_MINUTE
import zel.util.MILLIS_PER_SECOND
import zel.wired.condition.RecentActions
import zel.wired.effect.user.CopyStore
import zel.wired.selector.GameSelectorFacts
import zel.wired.store.WiredWriter
import java.util.concurrent.atomic.AtomicReference

internal class CoreWiring(
    private val services: RoomEngineServices,
) {
    /** 0 writes a room's state only at the unload and the shutdown. */
    val roomSaveMillis =
        services.config
            .getInt(RoomCheckpoints.INTERVAL_KEY, RoomCheckpoints.DEFAULT_INTERVAL_MINUTES)
            .coerceAtLeast(0) * MILLIS_PER_MINUTE
    val roomWrites = OrderedRoomWrites(services.persistInOrder, holdMillis = roomSaveMillis)
    val nowSeconds: () -> Int = { (System.currentTimeMillis() / MILLIS_PER_SECOND).toInt() }
    val floorData = FloorStore(services.dataSource)
    val furniData = FurniStore(services.dataSource)
    val furniSaves = FurniSaves(furniData, roomWrites)
    val roomStore = RoomStore(services.dataSource)
    val playerCounts = PlayerCounts(roomStore::savePlayerCount, services.persistInOrder)
    val groupRooms = GroupRoomStore(services.dataSource)
    val transitStore = TransitStore(services.dataSource)
    val copies = CopyStore(services.dataSource, furniData)
    val tradeMessages = TypedTradeMessages()
    val wiredWriter: WiredWriter = WiredWriter.of(roomWrites)
    val furniInventory = PlayerFurniInventory(services.clients::onlinePlayer, services.inventoryItem)
    val rollers =
        Rollers(
            services.config.getInt(Rollers.MAX_UNITS_KEY, 1),
            rules.rollerDefaultSpeed,
            rules.rollerHeightTolerance,
        )
    val entryStore = JdbcEntryStore(services.dataSource)
    val entryReader = EntryReader(entryStore)
    val furniAccess = StoredFurniAccess(services.dataSource, GroupRights(entryStore)::decorates)
    val entryEnvironment =
        GameEntryEnvironment(
            services.clients,
            services.persistInOrder,
            nowSeconds,
            services.accountLevels,
        )
    val rooms = AtomicReference<RoomRegistry>()
    val leaving: RoomLeaving =
        RoomLeaving(
            { playerId, roomId -> rooms.get()?.leave(playerId, roomId) },
            entryEnvironment,
            entryStore,
            ownerLeft = ::ownerLeft,
        )
    val dances = Dances()
    val life: RoomLife =
        RoomLife(EntryLifeHooks(leaving, lifeHooks()), ::lifeTimes) { rooms.get()?.unloadIfEmpty(it) }
    val gameParts = AtomicReference<GamesPart>()
    val holds = TransitHolds(next = GameWalkHooks(gameParts::get, next = leaving))
    val walking =
        Walking(
            services.extensions.walkHooks(
                QuestWalkHooks(
                    services.clients::onlinePlayer,
                    services.persist,
                    LifeWalkHooks(life, next = DanceWalkHooks(dances, holds)),
                ),
            ),
            afterSteps = rollers::afterSteps,
            publicDoorLets = { services.config.getBoolean(PUBLIC_DOOR_KICK_KEY, false) },
            stepLimits = {
                PathSearch.StepLimits(
                    services.config.getDouble(STEP_HEIGHT_KEY, PathSearch.MAX_STEP_UP),
                    services.config.getBoolean(STEP_FALLING_KEY, true),
                )
            },
        )
    val unitsOnTiles = UnitsOnTiles { scope, old, new -> walking.furniChanged(scope, (old + new).distinct()) }

    /**
     * Wired furni moves: furni lifted under a standing unit leaves it at its height; a furni a move lands on a
     * standing unit's tile puts him onto it when he can stand on it.
     */
    val wiredUnitsOnTiles =
        object : UnitsOnTiles {
            override fun furniChanged(
                scope: RoomScope,
                oldTiles: List<Point>,
                newTiles: List<Point>,
            ) = walking.furniChanged(scope, (oldTiles + newTiles).distinct(), lifts = false)

            override fun furniLanded(
                scope: RoomScope,
                oldTiles: List<Point>,
                newTiles: List<Point>,
            ) = walking.furniChanged(scope, (oldTiles + newTiles).distinct(), lifts = false, onto = newTiles)
        }
    val heads = HeadTurns(life::asleep)
    val units = BotsAndPets(services, walking, life, heads, roomWrites)
    val trades =
        RoomTrades(
            walking,
            tradeMessages,
            TradeStore(services.dataSource) { services.config.getBoolean(LOG_TRADES_KEY, true) },
            { rules.tradeCreditFurni },
            services.progress,
            nowSeconds,
            services.persist,
        ) { roomId, job -> rooms.get()?.post(roomId, 0, job) }
    val words =
        RoomWords(
            JdbcRoomWordStore(services.dataSource),
            services.persistInOrder,
            { rules.wordFilterReplacement },
            { rules.roomMaxFilterWords },
        )
    val recent = RecentActions()
    val selectorFacts =
        GameSelectorFacts(
            leaving::doorOf,
            life,
            dances,
            {
                groupId,
                playerId,
                ->
                services.clients
                    .onlinePlayer(playerId)
                    ?.stats
                    ?.guildIds
                    ?.contains(groupId) == true
            },
            recent,
            otherName = { scope, unit ->
                when (unit.kind) {
                    is UnitKind.Bot -> {
                        units.bots
                            .byIndex(scope, unit.index)
                            ?.bot
                            ?.name
                    }

                    is UnitKind.Pet -> {
                        units.pets
                            .byIndex(scope, unit.index)
                            ?.pet
                            ?.name
                    }

                    is UnitKind.Player -> {
                        null
                    }
                }
            },
        ) { room, playerId ->
            gameParts
                .get()
                ?.of(room)
                ?.roomTeamOf(playerId)
                ?.code ?: 0
        }
    val arrivals = ArrivalNotes()
    val rightsStore = JdbcRightsStore(services.dataSource)
    val mutes = RoomMutes(rightsStore, nowSeconds)

    fun levels(room: Room): Map<Int, Int> = leaving.doorOf(room).present.mapValues { it.value.level }

    fun playerName(
        scope: RoomScope,
        playerId: Int,
    ): String? =
        leaving
            .doorOf(scope.room)
            .present[playerId]
            ?.card
            ?.name

    /** Built before [units]; reaches it only once a player leaves. */
    private fun ownerLeft(
        scope: RoomScope,
        playerId: Int,
    ) {
        units.pets.ownerLeft(scope, playerId)
    }

    private fun lifeHooks(): LifeHooks =
        DanceLifeHooks(
            dances,
            ServerLifeHooks(services.listings::categoriesAsRead, services.progress, services.clients::onlinePlayer),
        )

    private fun lifeTimes(): LifeTimes =
        LifeTimes(
            rules.sleepSeconds * LifeTiming.TICKS_PER_SECOND,
            rules.autoSleep,
            services.config.getBoolean(DECO_HOSTING_KEY, true),
        )

    private companion object {
        const val PUBLIC_DOOR_KICK_KEY = "room.public.door_tile_leaves"
        const val STEP_HEIGHT_KEY = "room.walk.max_step_up"
        const val STEP_FALLING_KEY = "room.walk.drops_any_height"
        const val LOG_TRADES_KEY = "trading.trades.are_logged"
        const val DECO_HOSTING_KEY = "room.hosting.is_counted"
    }
}
