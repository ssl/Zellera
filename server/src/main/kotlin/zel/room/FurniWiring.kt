package zel.room

import zel.game.items.ItemDataUrls
import zel.game.permissions.Privilege
import zel.room.entry.DoorCheck
import zel.room.entry.EntryViewerStates
import zel.room.entry.RoomEntry
import zel.room.entry.RoomEntryMethod
import zel.room.furni.FurniBehaviour
import zel.room.furni.FurniBehaviours
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.special.GameSpecialHost
import zel.room.furni.behaviour.special.JdbcDimmerStore
import zel.room.furni.behaviour.special.SpecialPackets
import zel.room.furni.behaviour.special.SpecialPart
import zel.room.furni.behaviour.state.ControllerViews
import zel.room.furni.behaviour.state.EffectProviderTile
import zel.room.furni.behaviour.state.FurniListeners
import zel.room.furni.behaviour.state.GameStateHooks
import zel.room.furni.behaviour.state.GuildFurni
import zel.room.furni.behaviour.state.PlantFurni
import zel.room.furni.behaviour.state.PlantStore
import zel.room.furni.behaviour.state.PuzzleBox
import zel.room.furni.behaviour.state.Pyramid
import zel.room.furni.behaviour.state.SoundMachine
import zel.room.furni.behaviour.state.SoundMachineStore
import zel.room.furni.behaviour.state.StatePackets
import zel.room.furni.behaviour.state.StatePart
import zel.room.furni.behaviour.state.Trap
import zel.room.furni.behaviour.state.VoteCounter
import zel.room.furni.behaviour.state.WaterCan
import zel.room.furni.buildpass.BuildPassArea
import zel.room.furni.place.FurniMoving
import zel.room.furni.place.FurniPickup
import zel.room.furni.place.FurniPlacing
import zel.room.furni.place.PickupConfirm
import zel.room.furni.place.RoomDecoCount
import zel.room.furni.place.RoomLimits
import zel.room.furni.place.StackHelperControl
import zel.room.furni.roller.QueueSpeed
import zel.room.furni.transit.PlayerTransitHooks
import zel.room.furni.transit.TransitPackets
import zel.room.furni.transit.TransitPart
import zel.room.protocol.FurniPackets
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.pet.GameSpecialPets
import zel.util.SECONDS_PER_HOUR

internal class FurniWiring(
    private val services: RoomEngineServices,
    private val core: CoreWiring,
    behaviourOf: (String) -> FurniBehaviour,
    private val entry: () -> RoomEntry?,
) {
    val buildPass = BuildPassArea(services, core.roomWrites)
    val transit =
        TransitPart(
            core.walking,
            core.holds,
            PlayerTransitHooks(
                services.clients::onlinePlayer,
                core.life::effect,
            ) { scope, unit, roomId, furniId -> travel(scope, unit, roomId, furniId) },
            core.transitStore,
            guildFields = services.guildFields,
            reads = RoomReads.of(services.persist) { roomId, job -> core.rooms.get()?.post(roomId, 0, job) },
        )
    val state =
        StatePart(
            GameStateHooks(
                core.walking,
                core.life,
                holds = core.holds,
                save = core.furniSaves::saveDirty,
                rights = { scope, playerId -> core.leaving.doorOf(scope.room).levelOf(playerId) >= DoorCheck.RIGHTS },
                gender = {
                    services.clients
                        .onlinePlayer(it)
                        ?.profile
                        ?.gender
                },
                progress = services.progress,
                unkickable = { services.clients.onlinePlayer(it)?.hasPermission(Privilege.ROOM_KICK_IMMUNE) == true },
                blocksEffect = { playerId, effect ->
                    val rank =
                        services.clients
                            .onlinePlayer(playerId)
                            ?.profile
                            ?.rank
                            ?.id ?: 0
                    services.permissions()?.blocksEffect(effect, rank) == true
                },
            ),
        )
    val controllerViews = ControllerViews(state)
    val special =
        SpecialPart(
            state.hooks,
            GameSpecialHost(
                services.dataSource,
                services.clients,
                services.items,
                { services.config.getBoolean(DIMMER_COLOUR_CHECK_KEY, true) },
                { owner -> services.accountLevels()?.inputChanged(owner) },
            ),
            persist = services.persist,
            post = { roomId, job -> core.rooms.get()?.post(roomId, block = job) },
            behaviourOf = behaviourOf,
            site = { ItemDataUrls.site(services.config) },
            dimmerStore = JdbcDimmerStore(services.dataSource),
            pets = GameSpecialPets(core.units.pets) { services.items()?.pets() },
            writes = core.roomWrites,
        )
    private val votes = VoteCounter(state)
    val soundMachine =
        SoundMachine(state, SoundMachineStore.jdbc(services.dataSource)) { services.items()?.items()?.jukeboxTrack(it) }
    val listeners = FurniListeners(state, votes, services.units)
    private val limits = RoomLimits()
    private val viewerStates = EntryViewerStates(controllerViews)
    private val placedCount =
        RoomDecoCount(services.clients::onlinePlayer, services.achievements, services::progressLater)

    val behaviours: List<Pair<String, FurniBehaviour>> =
        transit.behaviours + state.behaviours + special.behaviours +
            listOf(
                QueueSpeed.INTERACTION to QueueSpeed(),
                "group.furni" to GuildFurni(state, services.guildFields),
                "state.pyramid" to Pyramid(state, ::pyramidPause),
                "effect.trap" to Trap(state),
                "effect.provider" to EffectProviderTile(state),
                "counter.votes" to votes,
                "music.soundmachine" to soundMachine,
                "puzzle.box" to PuzzleBox(state),
                "plant.wateringcan" to WaterCan(state),
                "plant.pot" to plants(),
            )

    /** Runs on the room's write lane, behind the write that placed the furni. */
    fun deleteSpent(
        roomId: Int,
        rowId: Int,
        onDeleted: () -> Unit = {},
        onFailed: () -> Unit = {},
    ) {
        core.roomWrites.write(
            roomId,
            Runnable { if (special.host.deleteSpent(roomId, rowId)) onDeleted() else onFailed() },
        )
    }

    fun pickups(
        confirm: PickupConfirm,
        onPicked: (RoomScope, RoomFurni) -> Unit = { _, _ -> },
    ): FurniPickup =
        FurniPickup(
            core.furniInventory,
            core.copies,
            core.roomWrites,
            core.unitsOnTiles,
            confirm,
            buildPass.picks,
            { roomId, block -> core.rooms.get()?.post(roomId, block = block) },
            { scope, furni ->
                state.release(scope, furni)
                onPicked(scope, furni)
            },
        )

    fun placingPackets(
        registry: RoomRegistry,
        behaviours: FurniBehaviours,
        pickups: FurniPickup,
        use: FurniUseWiring,
    ): FurniPackets =
        FurniPackets(
            registry,
            core.furniAccess,
            FurniPlacing(
                core.furniInventory,
                core.furniData,
                core.roomWrites,
                behaviours,
                limits = limits,
                units = core.unitsOnTiles,
                viewerStates = viewerStates,
                placedCount = placedCount,
                rentedBy = { room, player, tile -> use.rentableSpace.rentedBy(room, player, tile) },
                paint = use.paint::paint,
                unplaced = pickups::unplace,
            ),
            FurniMoving(core.unitsOnTiles) { room, player, tile -> use.rentableSpace.rentedBy(room, player, tile) },
            pickups,
            StackHelperControl(core.unitsOnTiles),
        )

    fun buildPassPackets(
        registry: RoomRegistry,
        behaviours: FurniBehaviours,
    ) = buildPass.packets(
        registry,
        core.furniAccess,
        core.furniData,
        behaviours,
        BuildPassArea.Placed(limits, core.unitsOnTiles, viewerStates, placedCount),
    )

    fun transitPackets(registry: RoomRegistry) = TransitPackets(registry, core.furniAccess, transit)

    fun statePackets(registry: RoomRegistry) = StatePackets(registry, state)

    fun specialPackets(registry: RoomRegistry) = SpecialPackets(registry, special)

    private fun travel(
        scope: RoomScope,
        unit: RoomUnit,
        roomId: Int,
        furniId: Int,
    ): Boolean {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return false
        val locked = services.config.getBoolean(TELEPORT_LOCKED_KEY, true)
        return travel(scope, playerId, roomId, furniId, locked, entryMethod = RoomEntryMethod.TELEPORTER)
    }

    /**
     * Opens [roomId] for the player on the game's thread pool, arriving on [furniId]; [pastDoor] skips any door mode.
     * [arrivalEffect] (0 for none) shows on the user at the end of the entry burst. [entryMethod] is what the user's
     * `@room_entry.method` reads in [roomId]. [afterwards] runs there once the entry is answered, whatever the answer.
     */
    fun travel(
        scope: RoomScope,
        playerId: Int,
        roomId: Int,
        furniId: Int,
        pastDoor: Boolean,
        arrivalEffect: Int = 0,
        entryMethod: Int = RoomEntryMethod.DEFAULT,
        afterwards: () -> Unit = {},
    ): Boolean {
        val player = services.clients.onlinePlayer(playerId) ?: return false
        val entry = entry() ?: return false
        core.arrivals.note(playerId, roomId, furniId, scope.now(), pastDoor, arrivalEffect, entryMethod)
        player.services.threading.run(
            Runnable {
                try {
                    entry.open(player, roomId, "")
                } finally {
                    afterwards()
                }
            },
        )
        return true
    }

    private fun plants(): PlantFurni =
        PlantFurni(
            state,
            PlantStore(services.dataSource),
            services.persist,
            { furni ->
                val known =
                    services
                        .items()
                        ?.items()
                        ?.sideTables
                        ?.plant(furni.definition.name)
                val states = furni.definition.stateCount
                PlantStore.Kind(known?.growCount ?: maxOf(0, states - 2), known?.deathCount ?: maxOf(1, states - 1))
            },
            { services.config.getInt(PLANT_WATER_KEY, PLANT_WATER_SECONDS) },
            { services.config.getInt(PLANT_DEATH_KEY, PLANT_DEATH_HOURS) * SECONDS_PER_HOUR },
        )

    private fun pyramidPause(): IntRange =
        services.config.getInt(
            PYRAMID_SHORTEST_KEY,
            Pyramid.DEFAULT_PAUSE.first,
        )..services.config.getInt(PYRAMID_LONGEST_KEY, Pyramid.DEFAULT_PAUSE.last)

    private companion object {
        const val DIMMER_COLOUR_CHECK_KEY = "furni.dimmer.takes_palette_only"
        const val TELEPORT_LOCKED_KEY = "furni.teleport.enters_locked_rooms"
        const val PYRAMID_SHORTEST_KEY = "furni.pyramid.min_pause.seconds"
        const val PYRAMID_LONGEST_KEY = "furni.pyramid.max_pause.seconds"
        const val PLANT_WATER_KEY = "furni.plant.watering.gap.seconds"
        const val PLANT_WATER_SECONDS = 1800
        const val PLANT_DEATH_KEY = "furni.plant.dries_out.hours"
        const val PLANT_DEATH_HOURS = 24
    }
}
