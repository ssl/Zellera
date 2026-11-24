package zel.game.achievements

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.orFallback

internal class AchievementBook(
    private val read: () -> Map<String, Achievement>,
    private val readTriggers: () -> List<AchievementTrigger> = { emptyList() },
) {
    @Volatile
    private var triggers: Map<String, List<Pair<String, Int>>> = emptyMap()

    @Volatile
    private var byCode: Map<String, Achievement> = emptyMap()

    @Volatile
    private var byId: Map<Int, Achievement> = emptyMap()

    val all: Map<String, Achievement> get() = byCode

    operator fun get(code: String): Achievement? = byCode[code]

    fun withId(id: Int): Achievement? = byId[id]

    /** A trigger without a detail counts every detail of its event; one with a detail only that one. */
    fun forEvent(event: String): List<Achievement> {
        val name = event.substringBefore(GameEvents.DETAIL)
        val detail = event.substringAfter(GameEvents.DETAIL, "")
        return triggers[name]
            .orEmpty()
            .filter { (wanted, _) -> wanted.isEmpty() || wanted == detail }
            .mapNotNull { (_, id) -> byId[id] }
            .distinct()
    }

    @Synchronized
    fun addTriggers(added: List<AchievementTrigger>) {
        val merged = triggers.toMutableMap()
        added.forEach { merged[it.event] = merged[it.event].orEmpty() + (it.detail to it.achievementId) }
        triggers = merged
    }

    @Synchronized
    fun add(achievement: Achievement) {
        publish(byCode + (achievement.name to achievement))
    }

    @Synchronized
    fun refresh() {
        val started = System.currentTimeMillis()
        val fresh = LOGGER.orFallback("loading the achievements", null) { read() } ?: return
        val loaded = LOGGER.orFallback("loading the achievement triggers", null) { readTriggers() } ?: return
        triggers = loaded.groupBy({ it.event }, { it.detail to it.achievementId })
        val current = byCode
        publish(
            fresh.mapValues { (code, achievement) ->
                current[code]?.also { it.refreshFrom(achievement) } ?: achievement
            },
        )
        LOGGER.info(
            "{} achievements with {} triggers loaded in {} ms",
            byCode.size,
            loaded.size,
            System.currentTimeMillis() - started,
        )
    }

    private fun publish(codes: Map<String, Achievement>) {
        byId = codes.values.associateBy { it.id }
        byCode = codes
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(AchievementBook::class.java)
    }
}
