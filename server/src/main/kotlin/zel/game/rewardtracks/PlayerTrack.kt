package zel.game.rewardtracks

/** Thread-safe. */
class PlayerTrack {
    private var points = 0
    private var pass = false
    private val progress = HashMap<String, Int>()
    private val claimed = HashSet<String>()

    internal data class Moved(
        val count: Int,
        val points: Int,
    )

    internal data class Claim(
        val result: ClaimResult,
        val completedTrack: Boolean = false,
    )

    @Synchronized
    fun points(): Int = points

    @Synchronized
    fun hasPass(): Boolean = pass

    @Synchronized
    fun progressOf(taskId: String): Int = progress[taskId] ?: 0

    @Synchronized
    internal fun progressSnapshot(): Map<String, Int> = progress.toMap()

    @Synchronized
    internal fun claimedSnapshot(): Set<String> = claimed.toSet()

    /** Counts [amount] on [task]; a holder's level points are multiplied by [pointsFactor], rounded down. */
    @Synchronized
    internal fun advance(
        task: RewardTrackDefinition.Task,
        amount: Int,
        pointsFactor: Double = 1.0,
        premiumOpen: Boolean = false,
    ): Moved? {
        val before = progressOf(task.id)
        val last = task.lastCount()
        if (before >= last) return null
        val after = minOf(last.toLong(), before.toLong() + amount).toInt()
        progress[task.id] = after
        points += (task.pointsBetween(before, after, premiumOpen) * pointsFactor).toInt()
        return Moved(after, points)
    }

    @Synchronized
    internal fun claim(
        track: RewardTrackDefinition,
        prize: RewardTrackDefinition.Prize,
        premiumOpen: Boolean,
        canPay: (RewardTrackDefinition.Prize) -> Boolean,
        persist: () -> Unit,
    ): Claim =
        when {
            prize.id in claimed -> {
                Claim(ClaimResult.ALREADY_CLAIMED)
            }

            prize.premium && !premiumOpen -> {
                Claim(ClaimResult.PREMIUM_REQUIRED)
            }

            points < prize.requiredPoints -> {
                Claim(ClaimResult.NOT_ENOUGH_POINTS)
            }

            !canPay(prize) -> {
                Claim(ClaimResult.FAILED)
            }

            else -> {
                val wasComplete = track.completedBy(claimed)
                persist()
                claimed.add(prize.id)
                Claim(ClaimResult.OK, !wasComplete && track.completedBy(claimed))
            }
        }

    @Synchronized
    internal fun buyPass(instantPoints: Int): Int {
        pass = true
        points += instantPoints
        return points
    }

    @Synchronized
    internal fun restorePoints(stored: Int) {
        points = stored
    }

    @Synchronized
    internal fun restoreProgress(
        taskId: String,
        count: Int,
    ) {
        progress[taskId] = count
    }

    @Synchronized
    internal fun restoreClaim(prizeId: String) {
        claimed.add(prizeId)
    }

    @Synchronized
    internal fun restorePass() {
        pass = true
    }
}
