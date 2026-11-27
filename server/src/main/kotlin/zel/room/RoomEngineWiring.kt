package zel.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.effects.EffectRoles
import zel.game.players.Player
import zel.protocol.navigator.RoomCreation
import zel.protocol.typed.PacketPackage
import zel.room.chat.ChatCommands
import zel.room.chat.DanceEntryViews
import zel.room.chat.DoorChatHooks
import zel.room.chat.FloodControl
import zel.room.chat.Gestures
import zel.room.chat.RoomChat
import zel.room.entry.CategoryPrompt
import zel.room.entry.EntryBurst
import zel.room.entry.EntryViews
import zel.room.entry.RoomEntry
import zel.room.entry.RoomEntryMethod
import zel.room.event.RoomEvent
import zel.room.furni.FurniBehaviour
import zel.room.furni.FurniBehaviours
import zel.room.furni.FurniPart
import zel.room.furni.FurniSightPart
import zel.room.furni.InventoryFurniStuff
import zel.room.furni.PlainFurni
import zel.room.furni.behaviour.state.AreaChat
import zel.room.furni.place.FurniPickup
import zel.room.games.GamePackets
import zel.room.life.EffectEntryViews
import zel.room.map.FloorPlanEditor
import zel.room.map.FloorPlanLimits
import zel.room.map.RoomFloors
import zel.room.port.EngineRoomClosing
import zel.room.port.EngineRoomCommands
import zel.room.port.EngineRoomPort
import zel.room.port.RoomPort
import zel.room.protocol.ChatPackets
import zel.room.protocol.EntryPackets
import zel.room.protocol.FloorPlanPackets
import zel.room.protocol.RightsPackets
import zel.room.protocol.RoomSidePackets
import zel.room.protocol.UnitPackets
import zel.room.quests.QuestListener
import zel.room.rights.Ignores
import zel.room.rights.Moderation
import zel.room.rights.ModerationHost
import zel.room.rights.MuteChatHooks
import zel.room.rights.MuteEntryViews
import zel.room.rights.RaidProtection
import zel.room.rights.RoomRights
import zel.room.rights.SettingsEditor
import zel.room.side.RoomQuestions
import zel.room.tick.ClientChannels
import zel.room.tick.RoomTick
import zel.room.tick.RoomWorkers
import zel.room.tick.TickPart
import zel.room.tick.TickParts
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.pet.PetStore
import zel.wired.effect.user.VisualEntryViews
import zel.wired.protocol.WiredEntryViews
import java.util.concurrent.atomic.AtomicReference

class RoomEngineWiring(
    private val services: RoomEngineServices,
) {
    fun build(): RoomEngine {
        val areas = Areas()
        val core = areas.core
        val registry = registry(areas).also(core.rooms::set)
        val moderation = Moderation(registry, core.leaving, core.rightsStore, moderationHost(core), core.mutes)
        val views = entryViews(areas, moderation)
        val handles =
            RoomHandles(
                registry,
                moderation,
                views,
                RoomRights(
                    registry,
                    core.leaving,
                    core.rightsStore,
                    services.persistInOrder,
                    views,
                    areas.pickups::ejectFurniOf,
                ),
                SettingsEditor(registry, core.rightsStore, core.roomStore::load, ::staffTags) { scope, settings ->
                    core.units.pets.switched(scope, PetStore.RoomSwitches(settings.allowPets, settings.muteAllPets))
                },
                RoomSidePackets(registry, core.entryStore, moderation, core.nowSeconds),
                roomEntry(areas, registry, views).also(areas.entries::set),
            )
        val port = roomPort(areas, handles).also(areas.port::set)
        startExtensions(areas, registry)
        services.items()?.inventoryStuff =
            InventoryFurniStuff({ checkNotNull(areas.behaviours.get()) }, areas.wired.chests::inventoryStuff)
        return RoomEngine(
            registry,
            rooms = port,
            packages = packages(areas, handles),
            furniSaved = core.furniSaves::handedIn,
            flushHeld = core.roomWrites::flushAll,
        )
    }

    private fun startExtensions(
        areas: Areas,
        registry: RoomRegistry,
    ) {
        val actions =
            ExtensionRoomActions(
                { registry },
                areas.core.walking,
                services.definitionByCode,
                { areas.behaviours.get()?.forType(it) ?: PlainFurni },
                { scope, unit ->
                    when (val kind = unit.kind) {
                        is UnitKind.Player -> {
                            services.clients
                                .onlinePlayer(kind.playerId)
                                ?.profile
                                ?.name
                        }

                        is UnitKind.Bot -> {
                            areas.core.units.bots
                                .byIndex(scope, unit.index)
                                ?.bot
                                ?.name
                        }

                        is UnitKind.Pet -> {
                            null
                        }
                    }
                },
                { scope, playerId, roomId -> areas.furni.travel(scope, playerId, roomId, 0, pastDoor = true) },
                { areas.core.units.bots },
            )
        services.extensions.start(actions, services.dataSource, services.persistInOrder, services.clients)
    }

    /** Every room area, each built once; late references close the cycles between them. */
    private inner class Areas {
        val behaviours = AtomicReference<FurniBehaviours>()
        val entries = AtomicReference<RoomEntry>()
        val port = AtomicReference<RoomPort>()
        private val behaviourOf: (String) -> FurniBehaviour = { behaviours.get()?.forType(it) ?: PlainFurni }
        val core = CoreWiring(services)
        val furni = FurniWiring(services, core, behaviourOf, entries::get)
        val wired = WiredEngineWiring(services, core, furni.state)
        val games = gamesPart(services, core, furni.state, wired, port::get)
        val pickups: FurniPickup = furni.pickups(wired.variables.pickupConfirm)
        val use = FurniUseWiring(services, core, furni, pickups, port::get, behaviourOf)
        val boxes =
            WiredBoxWiring(
                services,
                core,
                games,
                wired,
                WiredEffectWiring(services, core, furni.state, games, wired, behaviourOf) { scope, id, room, to, then ->
                    val effect = EffectRoles.of(EffectRoles.TELEPORT_ROOM)
                    furni.travel(
                        scope,
                        id,
                        room,
                        to,
                        pastDoor = true,
                        arrivalEffect = effect,
                        entryMethod = RoomEntryMethod.TELEPORT,
                        afterwards = then,
                    )
                },
            )
        val effects = boxes.effects
        val questions = RoomQuestions()
        val flood = FloodControl()
        val furniBehaviours =
            FurniBehaviours(
                furni.behaviours + games.behaviours + boxes.behaviours + use.behaviours,
                furni.state.fallback,
            ).also(behaviours::set)
        val furniPart = FurniPart(core.furniData, services.definitions, furniBehaviours, core.furniSaves)
        val checkpoints =
            RoomCheckpoints(core.roomSaveMillis) { room ->
                core.furniSaves.saveDirty(room)
                core.roomWrites.flush(room.info.id)
            }
        val wiredParts = wired.engine(boxes.catalog, boxes.forms)
    }

    private class RoomHandles(
        val registry: RoomRegistry,
        val moderation: Moderation,
        val views: EntryViews,
        val rights: RoomRights,
        val settings: SettingsEditor,
        val side: RoomSidePackets,
        val entry: RoomEntry,
    )

    private fun registry(areas: Areas): RoomRegistry {
        val workers =
            RoomWorkers(
                RoomWorkers.count(
                    services.config.getInt(RoomWorkers.WORKERS_KEY, 0),
                    Runtime.getRuntime().availableProcessors(),
                ),
            )
        log.info("room engine: {} room workers", workers.count)
        return RoomRegistry(
            infos = areas.core.roomStore::load,
            floors = RoomFloors(areas.core.floorData),
            parts = roomParts(areas),
            listeners = roomListeners(areas),
            tick = RoomTick(tickParts(areas)),
            workers = workers,
            channels = ClientChannels(services.clients),
            settle = areas.core.roomWrites::settle,
        )
    }

    private fun tickParts(areas: Areas): TickParts {
        val core = areas.core
        val wired = areas.wiredParts.engine
        return TickParts(
            openFlash = wired.openFlash,
            deferredJobs = TickPart.inOrder(areas.games.tick, areas.effects.user.teleports.tick),
            wiredTimers = wired.tick,
            closeFlash = wired.closeFlash,
            furni =
                TickPart.inOrder(
                    areas.furni.transit.tick,
                    areas.furni.state.tick,
                    areas.furni.special.tick,
                    areas.furniPart.tick,
                    areas.wired.scores.leaderboards.tick,
                    areas.wired.chests.tick,
                    areas.checkpoints.tick,
                ),
            decoHosting = core.life.decoHostingPart,
            players =
                core.life.playersPart(
                    TickPart.inOrder(
                        core.heads.tick,
                        core.units.tick,
                        areas.effects.errands.tick,
                        services.extensions.tick,
                        core.walking.tick,
                        areas.questions.tick,
                        FurniSightPart(areas.furni.controllerViews),
                    ),
                ),
            jukebox = areas.use.jukebox.tick,
            emptyRoom = core.life.emptyRoomPart,
            doorbell = core.leaving.tick,
        )
    }

    private fun roomParts(areas: Areas): List<RoomPart> {
        val core = areas.core
        val furni = areas.furni
        return listOf(
            areas.games.beforeFurniSave,
            furni.state.settleBeforeSave,
            areas.furniPart,
            areas.games,
            core.walking,
            furni.transit,
            furni.state,
            furni.special,
            core.rollers,
            core.words,
            core.heads,
            core.dances,
            areas.boxes.recent,
            core.trades,
            areas.use.jukebox,
            areas.use.gifts,
            areas.questions,
        ) + core.units.parts +
            listOf(
                core.life,
                core.leaving,
                core.mutes,
                areas.boxes.forms,
                areas.boxes.labels.labels,
            ) + areas.effects.parts + areas.wiredParts.parts + areas.checkpoints + services.extensions.part
    }

    private fun roomListeners(areas: Areas): List<RoomListener> {
        val core = areas.core
        return listOf(areas.wiredParts.engine, core.recent, core.playerCounts, areas.wired.variables.listener) +
            areas.wired.chests.listener +
            areas.wired.scores.listener +
            core.units.listeners +
            RoomListener { scope, event ->
                if (event is RoomEvent.Left) {
                    core.trades.left(scope, event.unitIndex)
                    core.dances.left(scope.room, event.unitIndex)
                }
            } +
            areas.games.listener +
            areas.furni.listeners +
            areas.effects.user.state +
            areas.effects.duties +
            QuestListener(services.clients::onlinePlayer) +
            areas.questions +
            services.extensions.listener
    }

    private fun entryViews(
        areas: Areas,
        moderation: Moderation,
    ): EntryViews =
        areas.wired.addons.entryViews(
            VisualEntryViews(
                areas.effects.user.state,
                WiredEntryViews(
                    areas.boxes.forms,
                    areas.boxes.labels.views(
                        areas.wired.scores.entryViews(
                            areas.core.units.entryViews(
                                MuteEntryViews(
                                    moderation,
                                    areas.furni.transit.entryViews(
                                        EffectEntryViews(
                                            areas.core.life,
                                            areas.use.jukebox.entryViews(
                                                DanceEntryViews(areas.core.dances, areas.furni.controllerViews),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun roomEntry(
        areas: Areas,
        registry: RoomRegistry,
        views: EntryViews,
    ): RoomEntry {
        val core = areas.core
        return RoomEntry(
            registry,
            core.walking,
            core.leaving,
            core.entryReader,
            core.entryStore,
            EntryBurst(registry, core.walking, core.leaving, views, services.extensions.arrivals),
            core.arrivals,
            core.entryEnvironment,
            logsEntries = { services.config.getBoolean(ENTER_LOGS_KEY, true) },
            categoryPrompt = CategoryPrompt(::needsCategory),
        )
    }

    private fun needsCategory(info: RoomInfo): Boolean {
        val listings = services.listings
        if (listings.category(info.categoryId) == null) return true
        return RoomCreation.personalSpace(listings.categoriesInOrder())?.id == info.categoryId
    }

    private fun packages(
        areas: Areas,
        handles: RoomHandles,
    ): List<PacketPackage> {
        val core = areas.core
        val furni = areas.furni
        val registry = handles.registry
        val gamePackets = GamePackets(registry, areas.games)
        return listOf(
            FloorPlanPackets(
                FloorPlanEditor(
                    registry,
                    core.floorData,
                    ::FloorPlanLimits,
                    emptyRoom = core.leaving::emptyForRebuild,
                    rebuilt = handles.entry::rebuilt,
                ),
            ),
            furni.placingPackets(registry, areas.furniBehaviours, areas.pickups, areas.use),
            furni.buildPassPackets(registry, areas.furniBehaviours),
            gamePackets.taggingLookTo(UnitPackets(registry, core.walking), core.walking),
            gamePackets,
            EntryPackets(handles.entry),
            furni.transitPackets(registry),
            furni.statePackets(registry),
            furni.specialPackets(registry),
            rightsPackets(core, handles),
            chatPackets(areas, handles),
            core.units.packets(registry),
            areas.use.packets(registry),
        ) +
            sidePackets(services, core, registry, handles.side, handles.settings, areas.use.postIts) +
            areas.boxes.packets(registry, handles.views, core.rightsStore::hasRights)
    }

    private fun rightsPackets(
        core: CoreWiring,
        handles: RoomHandles,
    ): RightsPackets =
        RightsPackets(
            handles.rights,
            handles.moderation,
            handles.settings,
            RaidProtection(core.rightsStore),
            Ignores { handles.registry.roomOf(it) != null },
            core.rightsStore,
        )

    private fun chatPackets(
        areas: Areas,
        handles: RoomHandles,
    ): ChatPackets {
        val core = areas.core
        val life = core.life
        val moderation = handles.moderation
        val highscores = areas.wired.scores.highscores
        val gestures =
            Gestures(
                core.dances,
                life::active,
                { scope, unit -> core.walking.sendNow(scope, listOf(unit)) },
                life::sleep,
            )
        val commands =
            ChatCommands(services.chat, core.walking::changePosture) { scope, playerId, text ->
                highscores.command(scope, playerId, text)
            }
        val chat =
            RoomChat(
                services.chat,
                DoorChatHooks(core.leaving, MuteChatHooks(moderation, AreaChat())),
                core.words,
                areas.flood,
                commands,
                gestures,
                core.heads,
                life::active,
            )
        return ChatPackets(handles.registry, chat, gestures, core.words)
    }

    private fun roomPort(
        areas: Areas,
        handles: RoomHandles,
    ): RoomPort {
        val core = areas.core
        val registry = handles.registry
        val environment = core.entryEnvironment
        val pickups = areas.pickups
        return EngineRoomPort(
            registry,
            core.leaving,
            handles.rights,
            storedLevel = { playerId, roomId ->
                val player = environment.player(playerId)
                val card = core.entryStore.card(roomId)
                if (player != null && card != null) core.entryReader.level(player, card) else null
            },
            write = { playerId, message ->
                services.clients.owner(playerId)?.send(message)
            },
            infos = core.roomStore::load,
            storeGroup = core.groupRooms::assign,
            settingsApplied = handles.settings::applied,
            card = { playerId -> environment.player(playerId)?.let(core.entryReader::card) },
            muted = { scope, playerId -> handles.moderation.muteLeft(scope, playerId) > 0 },
            iconShown = { scope, unit, id ->
                areas.wired.variables.variables
                    .iconShown(scope.room, unit, id)
            },
            commands = roomCommands(areas, handles),
            keepsRoom = areas.wired.variables.pickupConfirm::keepsRoom,
            buildPassRooms = areas.furni.buildPass.roomSync(registry)::ofOwner,
            questions = areas.questions,
            leftGame = areas.flood::forget,
            trades = { playerId ->
                registry.roomOf(playerId)?.let { roomId ->
                    registry.post(roomId) { scope ->
                        core.trades.tradeOf(scope.room, playerId)?.let { core.trades.close(scope, it, playerId) }
                    }
                }
            },
            respected = { scope, unit ->
                core.life.active(scope, unit)
                core.dances.stop(scope, unit)
            },
            closing =
                EngineRoomClosing(
                    pickups,
                    core.units.bots,
                    core.units.pets,
                    core.furniSaves,
                    core.roomWrites,
                    core.groupRooms,
                    handles.side,
                    environment::player,
                    services.persisted,
                ),
        )
    }

    private fun roomCommands(
        areas: Areas,
        handles: RoomHandles,
    ): EngineRoomCommands {
        val core = areas.core
        return EngineRoomCommands(
            handles.registry,
            core.leaving,
            core.walking,
            core.life,
            core.units.pets,
            handles.moderation,
            areas.wired.scores.highscores,
            areas.pickups,
            services.clients::onlinePlayer,
        )
    }

    private fun moderationHost(core: CoreWiring): ModerationHost =
        object : ModerationHost {
            override fun player(id: Int) = core.entryEnvironment.player(id)

            override fun persistInOrder(
                key: Any,
                job: Runnable,
            ) = services.persistInOrder(key, job)

            override fun nowSeconds(): Int = core.nowSeconds()

            override fun reaches(
                staff: Player,
                targetId: Int,
            ) = staff.services.game.moderationService
                ?.reach
                ?.canModerate(staff, targetId) == true

            override fun walkOut(
                scope: RoomScope,
                unit: RoomUnit,
            ) {
                core.walking.sendOut(scope, unit)
            }

            override fun progress(
                playerId: Int,
                achievement: String,
                amount: Int,
            ) = services.progress(playerId, achievement, amount)
        }

    private fun staffTags(): Set<String> =
        services.config
            .getValue(STAFF_TAGS_KEY, "")
            .split(';')
            .filter { it.isNotEmpty() }
            .toSet()

    private companion object {
        const val STAFF_TAGS_KEY = "room.tags.staff_only"
        const val ENTER_LOGS_KEY = "room.visits.are_logged"
        val log: Logger = LoggerFactory.getLogger(RoomEngineWiring::class.java)
    }
}
