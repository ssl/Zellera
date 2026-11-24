package zel.room.unit.pet

import zel.game.pets.Pet
import zel.game.pets.PetCommand
import zel.room.map.Point

class RoomPet(
    val pet: Pet,
    val unitIndex: Int,
    val ownerName: String,
) {
    sealed interface Next {
        /** The placement greeting, in the tick after the RoomUnits entry. */
        data object Greet : Next

        data object AfterSmile : Next

        data object EndCommand : Next

        data class Clear(
            val posture: String,
        ) : Next

        data class EntrySecondGreeting(
            val greetedAt: Long,
        ) : Next

        data class EntryMood(
            val greetedAt: Long,
        ) : Next

        data object ClearGesture : Next

        data object PlantStage : Next

        /** A revived plant's climb to the stage its clock reached, one stage a step; [first] also sends its status. */
        data class ReviveStage(
            val stage: Int,
            val first: Boolean,
        ) : Next
    }

    data class Heard(
        val command: PetCommand,
        val ownerIndex: Int,
    )

    var heard: Heard? = null

    var next: Next? = null
    var dueAt: Long = 0

    var announcedGoal: Point? = null

    var nextActionAt: Long = 0

    var nextChatAt: Long = 0

    var heldUntil: Long = 0

    var muted: Boolean = false

    var lastCommandId: Int = -1
    var lastCommandAt: Long = NO_COMMAND_YET
    var sameCommandCount: Int = 0

    var meal: PetMeals.Meal? = null

    /** The breeding nest the pet was sent to, on its way there and while it sits in it; it takes no other walk. */
    var nestId: Int? = null

    /** The ball the pet is walking to. */
    var ballId: Int? = null

    var moodShownAt: Long = 0
    var gestureShownAt: Long? = null

    /** The sad face's silent line is due in the next tick. */
    var sadLineDue: Boolean = false

    /** When thirst last grew; 0 until the first tick. */
    var thirstGrewAt: Long = 0

    /** When walking last cost a happiness; 0 until the first tick. */
    var happinessLostAt: Long = 0

    var restingTired: Boolean = false

    /** When the energy regain last counted; 0 until the first tick. */
    var energyRegainedAt: Long = 0

    var toyId: Int? = null
    var toyUntil: Long = 0

    var rider: Int? = null

    var jump: HorseJumps.Jump? = null

    val plant = PlantLife.Show()

    /** Another owner's open ask to breed with this plant. */
    var proposal: PlantBreeding.Proposal? = null

    fun schedule(
        what: Next,
        at: Long,
    ) {
        next = what
        dueAt = at
    }

    private companion object {
        /** Far enough back that the first command is past every gap, without overflowing the subtraction. */
        const val NO_COMMAND_YET = Long.MIN_VALUE / 2
    }
}
