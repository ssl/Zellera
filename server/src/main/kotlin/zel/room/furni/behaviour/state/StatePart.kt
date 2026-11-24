package zel.room.furni.behaviour.state

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.entry.EntryMessages
import zel.room.furni.FurniBehaviour
import zel.room.furni.PlainFurni
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.use.ColorWheel
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** Every member runs under the room's lock. */
class StatePart(
    val hooks: StateHooks,
    val random: Random = Random.Default,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, RoomStates>()

    private val multistate = Multistate(this)
    private val untoggled = Multistate(this, toggles = false)
    private val unused = FixedState(this)

    val fallback: (String) -> FurniBehaviour = { type ->
        when {
            type in WIRED_LAMPS -> multistate
            type.startsWith(WIRED_PREFIX) -> PlainFurni
            type in NEVER_USED -> unused
            type in NEVER_TOGGLED || type.startsWith(GAME_PREFIX) -> untoggled
            else -> multistate
        }
    }

    val behaviours: List<Pair<String, FurniBehaviour>> = behaviours()

    val tick: TickPart =
        TickPart { scope ->
            val states = of(scope.room)
            if (states.firstTickAt == null) states.firstTickAt = scope.now()
            val later = states.later
            val due = later.filter { scope.now() + SLACK_MILLIS >= it.due }
            later.removeAll(due)
            due.forEach { job -> scope.room.furni[job.furniId]?.let { job.run(scope, it) } }
        }

    fun of(room: Room): RoomStates = rooms.getOrPut(room) { RoomStates() }

    override fun load(room: Room) {
        rooms[room] = RoomStates()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    /** Runs before the furni are saved at unload: furni whose jobs will never run take their settled state. */
    val settleBeforeSave: RoomPart =
        object : RoomPart {
            override fun load(room: Room) = Unit

            override fun unload(scope: RoomScope) {
                val states = rooms[scope.room] ?: return
                states.later.forEach { job -> settle(scope, scope.room.furni[job.furniId], job) }
                states.later.clear()
            }
        }

    fun later(
        scope: RoomScope,
        furni: RoomFurni,
        millis: Long,
        run: (RoomScope, RoomFurni) -> Unit,
    ) = later(scope, furni, millis, null, run)

    /** [later] for a job that ends a passing state; the furni takes [settled] should the job never run. */
    fun later(
        scope: RoomScope,
        furni: RoomFurni,
        millis: Long,
        settled: String?,
        run: (RoomScope, RoomFurni) -> Unit,
    ) {
        of(scope.room).later += StateJob(scope.now() + millis, furni.id, settled, run)
    }

    fun laterOrDropped(
        scope: RoomScope,
        furni: RoomFurni,
        millis: Long,
        dropped: (RoomScope) -> Unit,
        run: (RoomScope, RoomFurni) -> Unit,
    ) {
        of(scope.room).later += StateJob(scope.now() + millis, furni.id, null, run, dropped)
    }

    fun pending(
        room: Room,
        furni: RoomFurni,
    ): Boolean = of(room).later.any { it.furniId == furni.id }

    fun release(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val states = of(scope.room)
        val dropped = states.later.filter { it.furniId == furni.id }
        states.later.removeAll(dropped)
        dropped.forEach { settle(scope, furni, it) }
        states.gym.remove(furni.id)
    }

    private fun settle(
        scope: RoomScope,
        furni: RoomFurni?,
        job: StateJob,
    ) {
        job.dropped(scope)
        val settled = job.settled ?: return
        if (furni != null) StateShow.set(furni, settled)
    }

    fun resendViewerStates(scope: RoomScope) {
        val views = ControllerViews(this)
        for (playerId in scope.room.units.playerIds()) {
            val states = EntryMessages.ConfigurationStates(views.configurationStates(scope, playerId))
            scope.out.send(Lane.Now, Audience.Player(playerId), EntryMessages.ConfigurationStatesCodec, states)
        }
    }

    private fun behaviours(): List<Pair<String, FurniBehaviour>> {
        val handItems = Dispenser(this, effects = false)
        return listOf(
            "default" to multistate,
            "wired.antenna" to multistate,
            "tile.click" to multistate,
            "state.fixed" to FixedState(this),
            FurniListeners.TYPE to FixedState(this),
            "state.random.click" to ClickRandomState(this),
            "state.random" to RandomState(this),
            "height.multi" to MultiHeight(this),
            "state.switch" to Switch(this),
            "plate.pressure" to PressurePlate(this, allTiles = false),
            "plate.pressure.group" to PressurePlate(this, allTiles = true),
            "plate.color" to ColorPlate(),
            "effect.tile" to EffectTile(this),
            "effect.gym" to GymEquipment(this),
            "effect.toggle" to EffectToggle(this),
            "effect.dispenser" to Dispenser(this, effects = true),
            "handitem.dispenser" to handItems,
            "handitem.tile" to HandItemTile(this, handItems),
            "vending.plain" to VendingMachine(this, effects = false),
            "vending.any_side" to VendingMachine(this, effects = false, anySide = true),
            "vending.effect" to VendingMachine(this, effects = true),
            RoomControllers.HAND_ITEM_BLOCK to Controller(this, viewers = true),
            RoomControllers.INVISIBLE_FURNI to Controller(this, viewers = true, hidesFurni = true),
            RoomControllers.CHOOSER_DISABLE to Controller(this, viewers = true, keepsOnPickup = true),
            RoomControllers.WIRED_DISABLE to Controller(this, viewers = false),
            "area.hide" to AreaHide(this),
            "area.mute" to MuteArea(this),
            "area.tent" to Tent(),
            "state.recolor" to Recolorable(this),
            "show.fireworks" to Fireworks(this),
            "show.ignite" to Ignite(this),
            "show.cannon" to Cannon(this),
            "counter.hockey" to HockeyScore(this),
            "water.area" to WaterArea(this),
            "chance.wheel" to ColorWheel(this, random),
            "crafting.altar" to CraftingAltar(this),
        )
    }

    private companion object {
        const val SLACK_MILLIS = 100L
        const val WIRED_PREFIX = "wf_"

        /** Wired-line furni that are plain lamps: a use toggles them and fires their state-changed stack. */
        val WIRED_LAMPS = setOf("wf_glowball")

        const val GAME_PREFIX = "game."

        val NEVER_TOGGLED =
            setOf(
                "game.football.ball",
                "crack.plain",
                "crack.master",
                "crack.monster",
                "crack.subscription",
                "gift.box",
                "mystery.box",
                "mystery.key",
                "pet.toy",
                "pet.tree",
                "pet.trampoline",
                "pet.nest",
                "pet.nest.breeding",
                "pet.seed",
                "plant.pot",
                "display.badge",
                "outfit.clothing",
                "puzzle.box",
                "effect.trap",
                "display.trophy",
                "water.float",
                "pet.obstacle",
                "pet.box",
                "totem.legs",
                "totem.head",
                "totem.planet",
                "game.tileclaim.tile",
                "game.tileclaim.sphere",
                "game.tileclaim.puck",
                "game.tileclaim.teleport",
                "game.freeze.block",
                "game.freeze.tile",
                "game.freeze.exit",
                "game.tag.ice.pole",
                "game.tag.ice.field",
                "game.tag.bunny.pole",
                "game.tag.bunny.field",
                "game.tag.skate.field",
                "game.football.gate",
            )

        /**
         * Furni a use leaves as they are, also never toggled by wired (pet food, skate rail): a use sends no state.
         * A bowl still empties as pets eat from it (PetMeals).
         */
        val NEVER_USED = setOf("pet.food", "walk.rail")
    }
}
