package zel.game.pets

import zel.game.items.Item
import java.util.Random

class PetKind(
    val type: Int,
    val name: String,
    val offspringType: Int,
    private val random: () -> Random,
    private val commons: SharedPetData,
    val behaviour: PetBehaviour = PetBehaviour.PLAIN,
    val productName: String = "",
) {
    var happyPostures: Array<String> = emptyArray()
    var idlePostures: Array<String> = emptyArray()

    val commands: MutableList<PetCommand> = ArrayList()
    val lines = PetLines()

    fun addPosture(
        mood: String,
        action: String,
    ) {
        when (mood) {
            "happy" -> happyPostures += action
            "random" -> idlePostures += action
        }
    }

    fun accepts(furni: Item): Boolean = commons.furniRules.allows(furni, type)

    fun says(situation: PetSituation?): String? {
        val all = linesFor(situation)
        return if (all.isEmpty()) null else all[random().nextInt(all.size)]
    }

    fun linesFor(situation: PetSituation?): List<String> {
        if (situation == null) return emptyList()
        val own = lines.of(situation)
        // the shared lines (pet_vocals type_id NULL) only join a type that has lines of its own: a type without any
        // keeps quiet and greets with the smile alone
        return if (type == MONSTERPLANT_TYPE || lines.isEmpty()) own else own + commons.lines.of(situation)
    }

    companion object {
        const val MONSTERPLANT_TYPE = 16
        const val NO_OFFSPRING = -1
    }
}
