package zel.game.achievements

import zel.game.players.Player

/** One instance per code for the whole run: a reload swaps its [details], so maps keyed by it stay valid. */
class Achievement(
    val id: Int,
    val name: String,
    category: AchievementCategory,
    details: Details,
    private val owner: AchievementProgress = AchievementProgress.NONE,
) {
    data class Details(
        val state: Short,
        val displayMethod: Int,
        val subcategory: String,
        val levels: Map<Int, AchievementLevel>,
    ) {
        val ordered: List<AchievementLevel> = levels.values.sortedBy { it.level }
    }

    @Volatile
    var category: AchievementCategory = category
        private set

    @Volatile
    var details: Details = details
        private set

    val state: Short get() = details.state

    /** Whether the content pack counts this achievement on [event]. */
    fun countsOn(event: String): Boolean = this in owner.forEvent(event)

    val displayMethod: Int get() = details.displayMethod
    val subcategory: String get() = details.subcategory
    val levels: Map<Int, AchievementLevel> get() = details.levels
    val lastLevel: AchievementLevel? get() = details.ordered.lastOrNull()

    fun levelAt(progress: Int): AchievementLevel? = details.ordered.lastOrNull { progress >= it.progressNeeded }

    fun levelAfter(level: Int): AchievementLevel? = details.ordered.firstOrNull { it.level > level }

    fun levelBefore(level: Int): AchievementLevel? = details.ordered.lastOrNull { it.level < level }

    fun isFinishedAt(progress: Int): Boolean = lastLevel?.let { progress >= it.progressNeeded } ?: false

    fun badgeCode(level: Int): String = AchievementBadges.code(name, levels.size, level)

    fun progress(
        player: Player?,
        amount: Int = 1,
    ) = owner.progress(player, this, amount)

    fun progressFor(
        playerId: Int,
        amount: Int = 1,
    ) = owner.progressFor(playerId, this, amount)

    fun announce(player: Player) = owner.announce(player, this)

    fun refreshFrom(fresh: Achievement) {
        category = fresh.category
        details = fresh.details
    }

    override fun toString(): String = "Achievement($id, $name)"
}
