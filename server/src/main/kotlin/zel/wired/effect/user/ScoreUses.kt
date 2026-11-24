package zel.wired.effect.user

class ScoreUses {
    private var round = -1
    private val used = HashMap<Int, Int>()

    /** False when [times] (0 = no limit) are used up. */
    fun take(
        currentRound: Int,
        holder: Int,
        times: Int,
    ): Boolean {
        if (round != currentRound) {
            round = currentRound
            used.clear()
        }
        val count = used[holder] ?: 0
        if (times > 0 && count >= times) return false
        used[holder] = count + 1
        return true
    }

    companion object {
        const val TEAM = 0
    }
}
