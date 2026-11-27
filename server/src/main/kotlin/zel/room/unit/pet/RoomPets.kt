package zel.room.unit.pet

import zel.game.achievements.GameEvents
import zel.game.games.GameAchievementCounters
import zel.game.permissions.Privilege
import zel.game.permissions.isAnyOwner
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetAchievements
import zel.game.pets.PetEvent
import zel.game.pets.PetMessages
import zel.game.pets.PetSituation
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PetAddedToInventoryOut
import zel.protocol.generated.PetAddedToInventoryOutCodec
import zel.protocol.generated.PetPlacingErrorOut
import zel.protocol.generated.PetPlacingErrorOutCodec
import zel.protocol.generated.PetRemovedFromInventoryOut
import zel.protocol.generated.PetRemovedFromInventoryOutCodec
import zel.protocol.generated.RoomUnitRemoveOut
import zel.protocol.generated.RoomUnitRemoveOutCodec
import zel.protocol.generated.RoomUnitsOut
import zel.protocol.generated.RoomUnitsOutCodec
import zel.protocol.outgoing.inventory.InventoryPet
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.chat.ChatKind
import zel.room.chat.ChatText
import zel.room.event.RoomEvent
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.bot.UnitHost
import zel.room.unit.bot.UnitSpeech
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.walk.Walking
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.hypot

@Suppress("TooManyFunctions") // the pets' room life in one place
class RoomPets(
    private val store: PetStore,
    val walking: Walking,
    val host: UnitHost,
    private val persist: (Runnable) -> Unit,
    private val writes: RoomWrites,
    private val maxPetsInRoom: () -> Int,
    val tuning: PetTuning = PetTuning(),
) : RoomPart {
    /** Shared by all rooms; a room's entry is only touched under that room's lock. */
    private val rooms = ConcurrentHashMap<Room, RoomPetSet>()

    val plants = PlantLife(this)
    val plantCare = PlantCare(this, store, persist)
    val growth = PetGrowth(this, persist)
    val meals = PetMeals(this, store, persist)
    val needs = PetNeeds(this)
    val toys = PetToys(this)
    val football = PetFootball()
    val nests = PetNests(this, store, persist)
    val breeding = PlantBreeding(this, store, persist)
    val horses = HorseCare(this, store, persist)
    val jumps = HorseJumps(this, ::horseJumped)

    lateinit var care: PetCare

    init {
        walking.walkedTiles.count = { playerId, tiles -> host.progress(playerId, GameEvents.HORSE_RIDDEN_TILES, tiles) }
        walking.walkedTiles.jumped = jumps::clean
        walking.walkedTiles.onBar = jumps::onBar
        walking.walkedTiles.inTheAir = jumps::holds
        walking.walkedTiles.railStepped = { playerId, effect, inARow ->
            GameAchievementCounters.skateRailCount(effect, inARow)?.let { (event, amount) ->
                host.progress(playerId, event, amount)
            }
        }
    }

    private class RoomPetSet(
        var switches: PetStore.RoomSwitches,
    ) {
        val byIndex = LinkedHashMap<Int, RoomPet>()
        val walkingBeside = mutableListOf<WalkingBeside>()
        val nests = HashMap<Int, PetNests.Nest>()
    }

    private class WalkingBeside(
        val unitIndex: Int,
        val goal: Point,
        val since: Long,
        val then: (RoomScope) -> Unit,
    )

    override fun load(room: Room) {
        val set = RoomPetSet(store.switches(room.info.id))
        store.loadRoom(room.info.id, host::pet).forEach { stored ->
            val unit =
                RoomUnit(
                    room.units.nextIndex(),
                    UnitKind.Pet(stored.pet.id),
                    stored.tile,
                    WalkTiles.at(room, stored.tile).height,
                    stored.direction,
                    stored.direction,
                )
            unit.statuses[PetLines.POSTURE] = PetLines.restingPosture(stored.pet)
            room.units.add(unit)
            walking.rest(room, unit)
            unit.changed = false // the entry burst carries the posture
            set.byIndex[unit.index] = RoomPet(stored.pet, unit.index, host.playerName(stored.pet.userId))
        }
        rooms[room] = set
    }

    override fun unload(scope: RoomScope) {
        val set = rooms.remove(scope.room) ?: return
        nests.unload(set.nests.values)
        val rows =
            set.byIndex.values.map { pet ->
                football.stop(pet)
                jumps.clear(pet)
                rowOf(pet.pet, unitOf(scope, pet), scope.room.info.id).also { scope.room.units.remove(pet.unitIndex) }
            }
        writes.write(scope.room.info.id, Runnable { store.saveAll(rows) })
    }

    /** Read off the lock, so placing and speaking never query them. */
    fun switched(
        scope: RoomScope,
        switches: PetStore.RoomSwitches,
    ) {
        petsOf(scope.room).switches = switches
    }

    fun all(scope: RoomScope): List<RoomPet> = petsOf(scope.room).byIndex.values.toList()

    fun byIndex(
        scope: RoomScope,
        index: Int,
    ): RoomPet? = byIndex(scope.room, index)

    fun byIndex(
        room: Room,
        index: Int,
    ): RoomPet? = petsOf(room).byIndex[index]

    fun byId(
        scope: RoomScope,
        petId: Int,
    ): RoomPet? = petsOf(scope.room).byIndex.values.firstOrNull { it.pet.id == petId }

    fun unitOf(
        scope: RoomScope,
        pet: RoomPet,
    ): RoomUnit? = scope.room.units[pet.unitIndex]

    fun entries(scope: RoomScope): RoomUnitsOut? {
        val entries = all(scope).mapNotNull { entryOf(scope, it) }
        return entries.takeIf { it.isNotEmpty() }?.let(PetEntry::roomUnits)
    }

    fun place(
        scope: RoomScope,
        player: Player,
        petId: Int,
        tile: Point,
    ) {
        val room = scope.room
        val playerId = player.profile.id
        val pet = player.inventory.pets[petId] ?: return
        val set = petsOf(room)
        val owner = room.info.ownerId == playerId
        val staff = player.isAnyOwner || player.hasPermission(Privilege.ROOM_PLACE_ANYWHERE)
        if (!owner && !set.switches.allowOtherPets && !staff) return placingError(scope, playerId, FORBIDDEN_IN_FLAT)
        if (set.byIndex.size >= maxPetsInRoom() && !player.hasPermission(Privilege.PETS_UNLIMITED)) {
            return placingError(scope, playerId, MAX_PETS)
        }
        val playerUnit = room.units.ofPlayer(playerId)
        val target =
            placementTile(scope, playerUnit, tile, owner) ?: return placingError(scope, playerId, NO_FREE_TILES)
        val state = WalkTiles.at(room, target).state
        if (state != TileState.Open && state !is TileState.Seat) return placingError(scope, playerId, TILE_TAKEN)

        val placed = putDown(scope, pet, target, 0)
        player.inventory.pets.remove(pet)
        val removed = PetRemovedFromInventoryOut(pet.id)
        scope.out.send(Lane.Now, Audience.Player(playerId), PetRemovedFromInventoryOutCodec, removed)
        placed.nextChatAt = scope.now() + SMILE_MILLIS
        placed.nextActionAt = scope.now() + SMILE_MILLIS
        save(scope, placed)
        if (pet is MonsterPlant) {
            faceFirst(scope, placed, playerUnit)
            val ownerId = room.info.ownerId
            val inRoom = set.byIndex.values.map { it.pet }
            persist(Runnable { PetAchievements.credit(pet.services, PetEvent.PlantsInRoom(ownerId, inRoom)) })
        } else {
            placed.schedule(RoomPet.Next.Greet, scope.now())
        }
        if (!owner) {
            persist(Runnable { PlayerActions.progress(player, CountedAction.PET_OTHER_USERS_ROOM, 1) })
        }
    }

    private fun faceFirst(
        scope: RoomScope,
        placed: RoomPet,
        playerUnit: RoomUnit?,
    ) {
        val unit = unitOf(scope, placed) ?: return
        playerUnit?.let { UnitDirection.between(unit.tile, it.tile) }?.let(unit::face)
        unit.changed = true
    }

    private fun mayAdd(
        scope: RoomScope,
        ownerId: Int,
    ): Boolean {
        val set = petsOf(scope.room)
        val isOwner = scope.room.info.ownerId == ownerId
        val player = host.player(ownerId)
        val staff = player != null && (player.isAnyOwner || player.hasPermission(Privilege.ROOM_PLACE_ANYWHERE))
        if (!isOwner && !set.switches.allowOtherPets && !staff) return false
        val unlimited = player != null && player.hasPermission(Privilege.PETS_UNLIMITED)
        return unlimited || set.byIndex.size < maxPetsInRoom()
    }

    fun planted(
        scope: RoomScope,
        plant: Pet,
        tile: Point,
    ) {
        if (!mayAdd(scope, plant.userId)) return toInventory(plant, acquired = false)
        val owner = Audience.Player(plant.userId)
        val removed = PetRemovedFromInventoryOut(plant.id)
        scope.out.send(Lane.Now, owner, PetRemovedFromInventoryOutCodec, removed)
        val placed = putDown(scope, plant, tile, 0)
        scope.out.send(Lane.Now, owner, PetRemovedFromInventoryOutCodec, removed)
        val unit = unitOf(scope, placed) ?: return
        plants.planted(scope, placed, unit)
        save(scope, placed)
        persist(Runnable { PlayerActions.progress(plant.userId, CountedAction.PLANT_MONSTER_PLANT, 1) })
    }

    fun hatched(
        scope: RoomScope,
        pet: Pet,
        tile: Point,
        direction: Int,
    ) {
        if (!mayAdd(scope, pet.userId)) return toInventory(pet, acquired = true)
        val placed = putDown(scope, pet, tile, direction)
        placed.nextChatAt = scope.now() + SMILE_MILLIS
        placed.nextActionAt = scope.now() + SMILE_MILLIS
        placed.schedule(RoomPet.Next.Greet, scope.now())
        save(scope, placed)
        persist(Runnable { acquired(pet) })
    }

    fun toInventory(
        pet: Pet,
        acquired: Boolean,
    ) {
        host.player(pet.userId)?.let { owner ->
            owner.inventory.pets.put(pet)
            owner.client?.let { PetMessages.send(it, PetAddedToInventoryOutCodec, inventoryEntry(pet, false)) }
        }
        if (acquired) acquired(pet)
    }

    private fun acquired(pet: Pet) {
        host.progress(pet.userId, GameEvents.PET_ACQUIRED, 1)
        PetAchievements.credit(pet.services, PetEvent.PetAcquired(pet.userId, pet))
    }

    fun putDownBaby(
        scope: RoomScope,
        baby: Pet,
        tile: Point,
        direction: Int,
    ) {
        if (!mayAdd(scope, baby.userId)) return toInventory(baby, acquired = false)
        val placed = putDown(scope, baby, tile, direction)
        placed.nextChatAt = scope.now() + SMILE_MILLIS
        placed.nextActionAt = scope.now() + SMILE_MILLIS
        save(scope, placed)
    }

    fun <T> toOwner(
        scope: RoomScope,
        ownerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        if (scope.room.units.ofPlayer(ownerId) != null) {
            scope.out.send(Lane.Now, Audience.Player(ownerId), codec, message)
        } else {
            host.player(ownerId)?.client?.let { PetMessages.send(it, codec, message) }
        }
    }

    fun nestsIn(scope: RoomScope): MutableMap<Int, PetNests.Nest> = petsOf(scope.room).nests

    fun walk(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
        goal: Point,
    ) {
        if (placed.nestId != null) return
        walking.letThrough(scope, unit, emptySet())
        walking.walkTo(scope, unit, goal)
    }

    fun walkBeside(
        scope: RoomScope,
        unit: RoomUnit,
        placed: RoomPet,
        then: (RoomScope) -> Unit,
    ) {
        val petTile = unitOf(scope, placed)?.tile ?: return
        if (maxOf(abs(unit.tile.x - petTile.x), abs(unit.tile.y - petTile.y)) == 1) return then(scope)
        val room = scope.room
        val goal =
            (0 until UnitDirection.COUNT)
                .map { petTile.step(it) }
                .filter { tile ->
                    room.floor.isFloor(tile) && WalkTiles.at(room, tile).state == TileState.Open &&
                        room.units.all().none { it.tile == tile }
                }.minByOrNull { hypot((it.x - unit.tile.x).toDouble(), (it.y - unit.tile.y).toDouble()) } ?: return
        walking.walkTo(scope, unit, goal)
        petsOf(room).walkingBeside += WalkingBeside(unit.index, goal, scope.now(), then)
    }

    fun runWalkedBeside(scope: RoomScope) {
        val waiting = petsOf(scope.room).walkingBeside
        val done =
            waiting.filter { wait ->
                val unit = scope.room.units[wait.unitIndex]
                val stopped = unit != null && !unit.isWalking
                val there = stopped && (unit.tile == wait.goal || scope.now() - wait.since >= WALK_START_MILLIS)
                unit == null || there
            }
        waiting.removeAll(done)
        done.filter { scope.room.units[it.unitIndex] != null }.forEach { it.then(scope) }
    }

    fun pickUp(
        scope: RoomScope,
        player: Player,
        petId: Int,
    ) {
        val placed = byId(scope, petId) ?: return
        if (placed.plant.composting) return
        val pet = placed.pet
        val playerId = player.profile.id
        val allowed =
            playerId == pet.userId || scope.room.info.ownerId == playerId ||
                player.isAnyOwner
        if (!allowed) return
        val receiver = if (playerId == pet.userId) player else host.player(pet.userId)
        val full =
            receiver != null && !receiver.hasPermission(Privilege.PETS_UNLIMITED) &&
                receiver.inventory.pets.count >= host.config(INVENTORY_KEY, DEFAULT_INVENTORY)
        if (full) {
            val text =
                host
                    .text(
                        FULL_KEY,
                        "",
                    ).replace("%limit%", host.config(INVENTORY_KEY, DEFAULT_INVENTORY).toString())
            if (text.isNotEmpty()) player.alert(text)
            return
        }
        dismountRider(scope, placed)
        remove(scope, placed)
        writes.write(scope.room.info.id, Runnable { store.save(rowOf(pet, null, 0)) })
        if (receiver == null) return
        receiver.inventory.pets.put(pet)
        val added = inventoryEntry(pet, receiver === player)
        scope.out.send(Lane.Now, Audience.Player(receiver.profile.id), PetAddedToInventoryOutCodec, added)
    }

    fun ownerLeft(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (scope.room.info.ownerId == playerId) return
        all(scope).filter { it.pet.userId == playerId && !it.plant.composting }.forEach { placed ->
            dismountRider(scope, placed)
            remove(scope, placed)
            val pet = placed.pet
            writes.write(scope.room.info.id, Runnable { store.save(rowOf(pet, null, 0)) })
            val owner = host.player(playerId) ?: return@forEach
            owner.inventory.pets.put(pet)
            val added = inventoryEntry(pet, true)
            scope.out.send(Lane.Now, Audience.Player(playerId), PetAddedToInventoryOutCodec, added)
        }
    }

    fun move(
        scope: RoomScope,
        petId: Int,
        tile: Point,
        direction: Int,
    ) {
        if (direction !in 0 until UnitDirection.COUNT) return
        val placed = byId(scope, petId) ?: return
        if (placed.rider != null) return
        val unit = unitOf(scope, placed) ?: return
        val state = WalkTiles.at(scope.room, tile).state
        if (state == TileState.Void || state == TileState.Blocked) return
        unit.path = emptyList()
        unit.tile = tile
        unit.z = WalkTiles.at(scope.room, tile).height
        unit.face(direction)
        unit.statuses.remove(UnitStatus.SIT)
        unit.statuses.remove(UnitStatus.LAY)
        unit.statuses.remove(PetLines.INSIDE)
        unit.changed = true
        if (state is TileState.Seat) {
            PetLife.walkOff(
                scope,
                this,
                placed,
                unit,
            )
        } else {
            walking.sendNow(scope, listOf(unit))
        }
        save(scope, placed)
    }

    private fun horseJumped(
        scope: RoomScope,
        rider: RoomUnit,
    ) {
        val playerId = (rider.kind as? UnitKind.Player)?.playerId ?: return
        host.progress(playerId, GameEvents.HORSE_JUMP_STREAK, 1)
        host.progress(playerId, GameEvents.HORSE_JUMP, 1)
        val horse = walking.horseOf(scope, rider)?.let { byIndex(scope, it.index) }?.pet ?: return
        val owner = scope.room.info.ownerId
        persist(Runnable { PetAchievements.credit(horse.services, PetEvent.HorseJumped(owner, playerId)) })
    }

    /**
     * A line the pet says by itself (mood, idle, greeting, rest, toys, meals). A sad pet keeps them to itself and
     * fires the says stacks only with the sad face's silent line ([sayNothing]).
     */
    fun say(
        scope: RoomScope,
        placed: RoomPet,
        type: PetSituation,
        lane: Lane = Lane.Now,
    ) {
        if (placed.pet !is MonsterPlant && needs.isSad(placed.pet)) return
        answer(scope, placed, type, lane)
    }

    /** A line that answers a player (command, scratch, level-up): said by a sad pet too. */
    fun answer(
        scope: RoomScope,
        placed: RoomPet,
        type: PetSituation,
        lane: Lane = Lane.Now,
    ) {
        if (placed.muted || petsOf(scope.room).switches.muteAllPets) return
        val text =
            placed.pet.kind
                ?.says(type)
                ?.takeIf { it.isNotEmpty() } ?: return
        val unit = unitOf(scope, placed) ?: return
        // a pet's line fires the says stacks with the pet as speaker, as a bot's does; a hiding stack keeps it back
        val says = RoomEvent.Says(unit.index, text, shout = false, styleId = PET_STYLE)
        scope.raise(says)
        if (says.hiding.hidden) return
        UnitSpeech.toRoom(scope, unit, ChatKind.Talk, text, PET_STYLE, gesture = ChatText.gesture(text), lane = lane)
    }

    /**
     * The line after a sad face: it fires the says stacks with the pet as speaker and reaches no one. It goes 0.5 s
     * after the face, also for a pet whose type has no lines. Its text is never seen: it is raised empty, so only
     * any-line boxes take it.
     */
    fun sayNothing(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        if (placed.muted || petsOf(scope.room).switches.muteAllPets) return
        val unit = unitOf(scope, placed) ?: return
        scope.raise(RoomEvent.Says(unit.index, "", shout = false, styleId = PET_STYLE))
    }

    /** Deletes the pet's row, on the persistence executor's thread. */
    fun deleteRow(petId: Int) = store.delete(petId)

    fun save(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val roomId = scope.room.info.id
        val row = rowOf(placed.pet, unitOf(scope, placed), roomId)
        writes.write(roomId, Runnable { store.save(row) })
    }

    private fun putDown(
        scope: RoomScope,
        pet: Pet,
        tile: Point,
        direction: Int,
    ): RoomPet {
        val room = scope.room
        val unit = RoomUnit(room.units.nextIndex(), UnitKind.Pet(pet.id), tile, 0.0, direction, direction)
        unit.statuses[PetLines.POSTURE] = PetLines.restingPosture(pet)
        room.units.add(unit)
        walking.place(scope, unit, tile)
        unit.changed = false
        val placed = RoomPet(pet, unit.index, host.playerName(pet.userId))
        petsOf(room).byIndex[unit.index] = placed
        entryOf(scope, placed)?.let {
            scope.out.send(Lane.Now, Audience.Room, RoomUnitsOutCodec, PetEntry.roomUnits(listOf(it)))
        }
        return placed
    }

    private fun entryOf(
        scope: RoomScope,
        placed: RoomPet,
    ): PetEntry? = unitOf(scope, placed)?.let { PetEntry(placed, it, placed.ownerName, placed.rider != null) }

    private fun placementTile(
        scope: RoomScope,
        playerUnit: RoomUnit?,
        asked: Point,
        owner: Boolean,
    ): Point? {
        val room = scope.room
        if ((asked.x != 0 || asked.y != 0) && owner) return asked.takeIf { room.floor.isFloor(it) }
        playerUnit ?: return room.floor.door.tile
        val front = playerUnit.tile.step(playerUnit.bodyDirection)
        if (WalkTiles.at(room, front).state == TileState.Open) return front
        return if (WalkTiles.at(room, playerUnit.tile).state ==
            TileState.Open
        ) {
            playerUnit.tile
        } else {
            room.floor.door.tile
        }
    }

    private fun dismountRider(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        val rider = placed.rider?.let { scope.room.units[it] } ?: return
        val horseUnit = unitOf(scope, placed) ?: return
        care.dismount(scope, rider, horseUnit)
    }

    fun remove(
        scope: RoomScope,
        placed: RoomPet,
    ) {
        football.stop(placed)
        jumps.clear(placed)
        nests.leave(scope, placed)
        petsOf(scope.room).byIndex.remove(placed.unitIndex)
        scope.room.units.remove(placed.unitIndex)
        val removal = RoomUnitRemoveOut(placed.unitIndex.toString())
        scope.out.send(Lane.Now, Audience.Room, RoomUnitRemoveOutCodec, removal)
    }

    private fun placingError(
        scope: RoomScope,
        playerId: Int,
        code: Int,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), PetPlacingErrorOutCodec, PetPlacingErrorOut(code))

    private fun petsOf(room: Room): RoomPetSet =
        rooms.getOrPut(room) { RoomPetSet(PetStore.RoomSwitches(allowOtherPets = false, muteAllPets = false)) }

    companion object {
        const val SMILE_MILLIS = 3000L

        private const val WALK_START_MILLIS = 1000L
        private const val PET_STYLE = 0
        private const val FORBIDDEN_IN_FLAT = 1
        private const val MAX_PETS = 2
        private const val TILE_TAKEN = 3
        private const val NO_FREE_TILES = 4
        private const val DEFAULT_INVENTORY = 25
        private const val INVENTORY_KEY = "player.inventory.max_pets"
        private const val FULL_KEY = "inventory.pets.full"

        fun inventoryEntry(
            pet: Pet,
            open: Boolean,
        ): PetAddedToInventoryOut {
            val data = InventoryPet.of(pet)
            return PetAddedToInventoryOut(
                petId = data.petId,
                name = data.name,
                typeId = data.typeId,
                paletteId = data.paletteId,
                color = data.color,
                breedId = data.breedId,
                customParts =
                    data.customParts.map {
                        PetAddedToInventoryOut.CustomPartsItem(
                            it.partId,
                            it.style,
                            it.color,
                        )
                    },
                level = data.level,
                rarityLevel = data.rarityLevel,
                openInventory = open,
            )
        }

        fun rowOf(
            pet: Pet,
            unit: RoomUnit?,
            roomId: Int,
        ): PetStore.Row =
            PetStore.Row(
                id = pet.id,
                roomId = if (unit == null) 0 else roomId,
                experience = pet.experience,
                energy = pet.energy,
                respect = pet.respect,
                x = unit?.tile?.x ?: 0,
                y = unit?.tile?.y ?: 0,
                z = unit?.z ?: 0.0,
                direction = unit?.bodyDirection ?: 0,
                thirst = pet.thirst,
                happiness = pet.happiness,
            )
    }
}
