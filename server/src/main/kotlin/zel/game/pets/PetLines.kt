package zel.game.pets

class PetLines {
    private val bySituation = HashMap<PetSituation, MutableList<String>>()

    fun add(
        situation: PetSituation,
        line: String,
    ) {
        bySituation.getOrPut(situation) { ArrayList() }.add(line)
    }

    fun of(situation: PetSituation): List<String> = bySituation[situation].orEmpty()

    fun isEmpty(): Boolean = bySituation.isEmpty()
}
