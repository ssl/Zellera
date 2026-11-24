package zel.wired.variable

import zel.game.pets.HorsePet
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetLevels
import zel.room.RoomScope
import zel.room.furni.BuildPass
import zel.room.furni.RoomFurni
import zel.room.furni.StackHelperState
import zel.room.furni.behaviour.special.BackgroundToner
import zel.room.furni.behaviour.state.CustomValues
import zel.room.furni.behaviour.state.Recolorable
import zel.room.furni.place.Interactions
import zel.room.trade.RoomTrades
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.util.GameZone
import zel.util.MILLIS_PER_SECOND
import zel.wired.Hundredths
import zel.wired.effect.furni.WallPlace
import zel.wired.firing.Firing
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.WeekFields

class InternalValues(
    val facts: VariableFacts,
    private val flags: (RoomScope) -> VariableSpace,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun read(
        on: VariableOn,
        id: String,
        holder: Holder,
    ): Long? = valuesOf(on, holder)[id]

    fun valuesOf(
        on: VariableOn,
        holder: Holder,
    ): Map<String, Long> =
        when (holder) {
            is Holder.Furni -> {
                on.scope.room.furni[holder.id]
                    ?.let { furni(on.scope, it) }
                    .orEmpty()
            }

            Holder.Room -> {
                global(on.scope)
            }

            Holder.Context -> {
                context(on.firing)
            }

            else -> {
                holder.unitIn(on.scope.room)?.let { unit(on.scope, it) }.orEmpty()
            }
        }

    private fun unit(
        scope: RoomScope,
        unit: RoomUnit,
    ): Map<String, Long> =
        linkedMapOf<String, Long>().apply {
            put("-200", unit.index.toLong())
            put("-205", typeOf(unit).toLong())
            when (val kind = unit.kind) {
                is UnitKind.Player -> player(scope, unit, kind.playerId, this)
                is UnitKind.Bot -> bot(unit, kind.botId, this)
                is UnitKind.Pet -> pet(scope, unit, kind.petId, this)
            }
            carried(scope, unit, this)
            held(scope, Holder.of(unit), this)
        }

    private fun typeOf(unit: RoomUnit): Int =
        when (unit.kind) {
            is UnitKind.Player -> TYPE_USER
            is UnitKind.Pet -> TYPE_PET
            is UnitKind.Bot -> TYPE_BOT
        }

    private fun player(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
        out: MutableMap<String, Long>,
    ) {
        out["-206"] = facts.gender(scope, playerId).toLong()
        out["-210"] = facts.accountLevel(scope, playerId).toLong()
        out["-211"] = facts.achievementScore(scope, playerId).toLong()
        rights(scope, playerId, out)
        place(unit, out)
        out["-266"] = unit.entryMethod.toLong()
        if (unit.entryTeleportId != 0) out["-267"] = unit.entryTeleportId.toLong()
        out["-270"] = playerId.toLong()
        facts.favouriteGroup(scope, playerId)?.let { out["-250"] = it.toLong() }
        facts.team(scope, playerId)?.let { team ->
            out["-237"] = team.score.toLong()
            out["-238"] = team.color.toLong()
            out["-239"] = team.type.toLong()
        }
        if (facts.muted(scope, playerId)) out["-243"] = 0
        out.putAll(facts.transaction(scope, playerId))
    }

    private fun rights(
        scope: RoomScope,
        playerId: Int,
        out: MutableMap<String, Long>,
    ) {
        val level = facts.rightsLevel(scope, playerId)
        if (facts.hasClub(playerId)) out["-220"] = 0
        if (level > 0) out["-221"] = 0
        if (level == GROUP_ADMIN) out["-222"] = 0
        if (level >= OWNER) out["-223"] = 0
    }

    private fun bot(
        unit: RoomUnit,
        botId: Int,
        out: MutableMap<String, Long>,
    ) {
        out["-206"] = NO_GENDER
        out["-210"] = 0
        out["-211"] = 0
        place(unit, out)
        out["-266"] = 0
        out["-272"] = botId.toLong()
    }

    private fun pet(
        scope: RoomScope,
        unit: RoomUnit,
        petId: Int,
        out: MutableMap<String, Long>,
    ) {
        out["-206"] = NO_GENDER
        out["-210"] = 0
        out["-211"] = 0
        place(unit, out)
        out["-266"] = 0
        out["-271"] = petId.toLong()
        val pet = facts.pet(scope.room, unit) ?: return
        out["-280"] = pet.userId.toLong()
        petStats(pet, out)
        (pet as? MonsterPlant)?.let { plant(it, out) }
        (pet as? HorsePet)?.let { horse(it, facts.horseRider(scope.room, unit), out) }
    }

    private fun petStats(
        pet: Pet,
        out: MutableMap<String, Long>,
    ) {
        val plant = pet as? MonsterPlant
        out["~400"] = (plant?.stage ?: pet.level).toLong()
        out["~401"] = pet.experience.toLong()
        out["~402"] = pet.energy.toLong()
        out["~403"] = pet.happiness.toLong()
        out["~410"] = (if (plant != null) PLANT_TOP_STAGE else PET_TOP_LEVEL).toLong()
        out["~411"] = (plant?.stageGoal() ?: experienceGoal(pet.level)).toLong()
        out["~412"] = pet.maxEnergy.toLong()
        out["~413"] = MAX_HAPPINESS
        out["~420"] = pet.respect.toLong()
        out["~430"] = pet.userId.toLong()
        out["~431"] = pet.created.toLong() * MILLIS_PER_SECOND
    }

    private fun experienceGoal(level: Int): Int = PetLevels.goalAt(level)

    private fun plant(
        plant: MonsterPlant,
        out: MutableMap<String, Long>,
    ) {
        out["~600"] = plant.rarity.toLong()
        out["~601"] = plant.body.toLong()
        out["~602"] = plant.hue.toLong()
        if (plant.isBreedable) out["~610"] = 0
        if (plant.isGrown && !plant.isDead) out["~611"] = 0
        if (plant.isDead) {
            out["~612"] = 0
            out["~613"] = 0
        }
        if (!plant.isGrown) out["~614"] = 0
        out["~615"] = plant.secondsToLive().toLong()
        out["~616"] = plant.secondsToGrow().toLong()
        out["~617"] = plant.lifespanSeconds().toLong()
    }

    private fun horse(
        horse: HorsePet,
        riderId: Int?,
        out: MutableMap<String, Long>,
    ) {
        if (horse.saddled) out["~500"] = 0
        riderId?.let {
            out["~501"] = 0
            out["~502"] = it.toLong()
        }
    }

    private fun place(
        unit: RoomUnit,
        out: MutableMap<String, Long>,
    ) {
        out["-230"] = unit.tile.x.toLong()
        out["-231"] = unit.tile.y.toLong()
        out["-234"] = (unit.tile.x.toLong() shl BYTE) or unit.tile.y.toLong()
        out["-232"] = unit.bodyDirection.toLong()
        out["-233"] = Hundredths.of(unit.z)
    }

    private fun carried(
        scope: RoomScope,
        unit: RoomUnit,
        out: MutableMap<String, Long>,
    ) {
        if (facts.handItemHeld(unit)) out["-240"] = facts.handItem(unit).toLong()
        facts.effect(unit).takeIf { it != 0 }?.let { out["-241"] = it.toLong() }
        if (facts.frozen(scope, unit)) out["-242"] = 0
        flags(scope).icons[unit.index]?.takeIf { it.second > clock() }?.let { out["-245"] = it.first.toLong() }
        facts.dance(unit).takeIf { it != 0 }?.let { out["-260"] = it.toLong() }
        unit.statuses[SIGN]?.toLongOrNull()?.let { out["-261"] = it }
        if (facts.asleep(unit)) out["-262"] = 0
        if (unit.showsStatus(RoomTrades.STATUS)) out["-244"] = 0
    }

    private fun held(
        scope: RoomScope,
        holder: Holder,
        out: MutableMap<String, Long>,
    ) {
        flags(scope).internalFlags.forEach { (id, holders) -> if (holder in holders) out[id] = 0 }
    }

    private fun furni(
        scope: RoomScope,
        furni: RoomFurni,
    ): Map<String, Long> =
        linkedMapOf<String, Long>().apply {
            put("-129", if (BuildPass.isClientId(furni.id)) 1 else 0)
            if (furni.isWall) wallPlace(scope, furni, this) else floorPlace(furni, this)
            // a wall item's @id is minus its id, the client's wall id
            put("-100", if (furni.isWall) -furni.id.toLong() else furni.id.toLong())
            put("-101", furni.definition.spriteId.toLong())
            put("-102", Hundredths.of(furni.height))
            put("-110", stateOf(furni))
            put("-122", rotationOf(furni))
            put("-180", furni.ownerId.toLong())
            put("-140", furni.definition.width.toLong())
            put("-141", furni.definition.length.toLong())
            furni.anchor?.let {
                put(
                    "-126",
                    (it.x.toLong() shl 2 * BYTE) or (it.y.toLong() shl BYTE) or rotationOf(furni),
                )
            }
            abilities(furni, this)
            kinds(furni, this)
            flags(scope).flights[furni.id]?.let { putAll(it.values()) }
            held(scope, Holder.of(furni), this)
        }

    private fun floorPlace(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val tile = furni.anchor ?: return
        out["-120"] = tile.x.toLong()
        out["-121"] = tile.y.toLong()
        out["-125"] = (tile.x.toLong() shl BYTE) or tile.y.toLong()
        out["-123"] = Hundredths.of(furni.z)
    }

    private fun wallPlace(
        scope: RoomScope,
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val place = WallPlace.of(furni) ?: return
        out["-120"] = place.tile.x.toLong()
        out["-121"] = place.tile.y.toLong()
        out["-125"] = (place.tile.x.toLong() shl BYTE) or place.tile.y.toLong()
        out["-123"] = place.altitude(scope.room).toLong()
        out["-190"] = place.localX.toLong()
    }

    private fun abilities(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val item = furni.definition
        if (item.params.split(',').any { it.trim() == INVISIBLE }) out["-124"] = 0
        if (item.canStack) out["-130"] = 0
        if (item.canWalk) out["-131"] = 0
        if (item.canSit) out["-132"] = 0
        if (item.canLay) out["-133"] = 0
    }

    private fun kinds(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val code = Interactions.of(furni.definition)
        when {
            code == "area.hide" -> {
                areaHide(furni, out)
            }

            code == "tile.walk" -> {
                if (StackHelperState.of(furni.state).multiWalk) out[InternalIds.MULTI_WALK] = 0
            }

            code == "room.toner" -> {
                toner(furni, out)
            }

            code == "transit.door" || code == "transit.tile" -> {
                facts.teleportTarget(furni)?.let { out[InternalIds.TELEPORT_TARGET] = it.toLong() }
            }

            code == "state.recolor" -> {
                recolorable(furni, out)
            }

            code.startsWith("wf_storage_") -> {
                out.putAll(facts.chest(furni))
            }

            facts.clock(furni) != null -> {
                clock(furni, out)
            }
        }
    }

    private fun areaHide(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val values = CustomValues.read(furni.state, AreaHideValues.DEFAULTS)

        fun long(key: String) = values[key]?.toLongOrNull() ?: 0
        out["~110"] = long(AreaHideValues.ROOT_X)
        out["~111"] = long(AreaHideValues.ROOT_Y)
        out["~112"] = long(AreaHideValues.WIDTH)
        out["~113"] = long(AreaHideValues.LENGTH)
        if (long(AreaHideValues.INVISIBLE) != 0L) out["~114"] = 0
        if (long(AreaHideValues.WALL_ITEMS) != 0L) out["~115"] = 0
        if (long(AreaHideValues.INVERT) != 0L) out["~116"] = 0
    }

    private fun toner(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val values = BackgroundToner.values(furni.state)
        out["~100"] = values[1].toLong()
        out["~101"] = values[2].toLong()
        out["~102"] = values[TONER_LIGHTNESS].toLong()
    }

    private fun recolorable(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val rgb = (furni.behaviour as? Recolorable)?.color(furni)?.toLong() ?: return
        out["~140"] = (rgb shr 2 * BYTE) and CHANNEL
        out["~141"] = (rgb shr BYTE) and CHANNEL
        out["~142"] = rgb and CHANNEL
        out["~146"] = rgb
    }

    private fun clock(
        furni: RoomFurni,
        out: MutableMap<String, Long>,
    ) {
        val clock = facts.clock(furni) ?: return
        out["~120"] = clockState(furni)
        out["~121"] = clock.millis.toLong() / HALF_SECOND
        if (clock.kind.runsGames) out["~122"] = 0
    }

    private fun clockState(furni: RoomFurni): Long {
        val clock = facts.clock(furni) ?: return 0
        return when {
            clock.paused -> CLOCK_PAUSED
            clock.running -> CLOCK_RUNNING
            else -> 0
        }
    }

    /** An area hide keeps its state among its values (one that is on reads 1). */
    private fun stateOf(furni: RoomFurni): Long =
        when {
            facts.clock(furni) != null -> {
                clockState(furni)
            }

            Interactions.of(furni.definition) == "area.hide" -> {
                CustomValues.read(furni.state, AreaHideValues.DEFAULTS)[AreaHideValues.STATE]?.toLongOrNull() ?: 0
            }

            else -> {
                furni.state.toLongOrNull() ?: 0
            }
        }

    private fun rotationOf(furni: RoomFurni): Long =
        if (furni.definition.hasDiagonalDirections()) furni.direction.toLong() else (furni.direction / 2).toLong()

    private fun global(scope: RoomScope): Map<String, Long> =
        linkedMapOf<String, Long>().apply {
            val room = scope.room
            put(
                "-300",
                room.furni
                    .all()
                    .size
                    .toLong(),
            )
            put(
                "-301",
                room.units
                    .playerIds()
                    .size
                    .toLong(),
            )
            put("-310", facts.wiredTimer(scope))
            TEAM_COLORS.forEachIndexed { i, color -> put(TEAM_SCORES[i], facts.teamScore(room, color).toLong()) }
            TEAM_COLORS.forEachIndexed { i, color -> put(TEAM_SIZES[i], facts.teamSize(room, color).toLong()) }
            put("-380", room.info.id.toLong())
            put("-381", room.info.groupId.toLong())
            times(this, ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock()), zone(facts.timezone(room))))
        }

    private fun times(
        out: MutableMap<String, Long>,
        time: ZonedDateTime,
    ) {
        out["-1000"] = time.toEpochSecond()
        out["-1001"] = time.get(ChronoField.MILLI_OF_SECOND).toLong()
        out["-1002"] = time.second.toLong()
        out["-1003"] = time.minute.toLong()
        out["-1004"] = time.hour.toLong()
        out["-1005"] = time.dayOfWeek.value.toLong()
        out["-1006"] = time.dayOfMonth.toLong()
        out["-1007"] = time.dayOfYear.toLong()
        out["-1008"] = time.get(WEEKS.weekOfYear()).toLong()
        out["-1009"] = time.monthValue.toLong()
        out["-1010"] = time.year.toLong()
    }

    private fun zone(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(GameZone.current)

    private fun context(firing: Firing?): Map<String, Long> {
        val run = firing ?: return emptyMap()
        val signal = run.signal
        return linkedMapOf<String, Long>().apply {
            run.context.forEach { (id, held) -> if (id.startsWith('-')) put(id, held.value) }
            put(
                "-400",
                run.selection.furniIds.size
                    .toLong(),
            )
            put(
                "-401",
                run.selection.unitIndexes.size
                    .toLong(),
            )
            put("-410", (signal?.furniIds?.size ?: 0).toLong())
            put("-411", (signal?.unitIndexes?.size ?: 0).toLong())
            signal?.takeIf { it.through != 0 }?.let { put("-420", it.through.toLong()) }
            (run.event as? zel.room.event.RoomEvent.Says)?.let {
                put("-450", if (it.shout) 1 else 0)
                put("-451", it.styleId.toLong())
            }
        }
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_PET = 2
        private const val TYPE_BOT = 4
        private const val NO_GENDER = -1L
        private const val GROUP_ADMIN = 3
        private const val OWNER = 4
        private const val BYTE = 8
        private const val PLANT_TOP_STAGE = 7
        private const val PET_TOP_LEVEL = 20
        private const val MAX_HAPPINESS = 100L
        private const val SIGN = "sign"
        private const val INVISIBLE = "is_invisible"
        private const val TONER_LIGHTNESS = 3
        private const val CHANNEL = 0xFFL
        private const val HALF_SECOND = 500L
        private const val CLOCK_RUNNING = 1L
        private const val CLOCK_PAUSED = 2L
        private val WEEKS: WeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1)
        private val TEAM_COLORS = listOf(1, 2, 3, 4)
        private val TEAM_SCORES = listOf("-320", "-321", "-322", "-323")
        private val TEAM_SIZES = listOf("-330", "-331", "-332", "-333")
    }
}
