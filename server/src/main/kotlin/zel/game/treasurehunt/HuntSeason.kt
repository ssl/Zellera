package zel.game.treasurehunt

/** Replaced whole by a reload, so a click is a map lookup and nothing in it changes afterwards. */
internal class HuntSeason(
    hunts: List<TreasureHunt>,
) {
    data class Hidden(
        val hunt: TreasureHunt,
        val clue: HuntClue,
    )

    private val behindFurni: Map<Int, Hidden>
    private val behindStep: Map<HuntStep, Hidden>

    init {
        val furni = HashMap<Int, Hidden>()
        val steps = HashMap<HuntStep, Hidden>()
        for (hunt in hunts) {
            for (clue in hunt.clues) {
                when (clue) {
                    is HuntClue.Furni -> furni[clue.furniId] = Hidden(hunt, clue)
                    is HuntClue.Step -> steps[HuntStep(hunt.code, clue.code)] = Hidden(hunt, clue)
                }
            }
        }
        behindFurni = furni
        behindStep = steps
    }

    fun behind(furniId: Int): Hidden? = behindFurni[furniId]

    fun behind(
        huntCode: String,
        stepCode: String,
    ): Hidden? = behindStep[HuntStep(huntCode, stepCode)]

    companion object {
        val EMPTY = HuntSeason(emptyList())
    }
}
