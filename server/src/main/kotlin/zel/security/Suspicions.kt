package zel.security

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.Sql
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

interface SuspicionLog {
    fun record(
        playerId: Int?,
        address: String?,
        device: String?,
        kind: Suspicion,
        detail: String = "",
    )

    fun muted(playerId: Int): Boolean

    fun forget(playerId: Int) = Unit

    object None : SuspicionLog {
        override fun record(
            playerId: Int?,
            address: String?,
            device: String?,
            kind: Suspicion,
            detail: String,
        ) = Unit

        override fun muted(playerId: Int): Boolean = false
    }
}

enum class Suspicion(
    val key: String,
    val weight: Int,
) {
    UNKNOWN_ID("net.unknown_id", STRONG),
    TICKET_ADDRESS("login.ticket_address", CLEAR),
    RELEASE("login.release", CLEAR),
    RAID("room.raid", WEAK),
    FLOOD("net.flood", WEAK),
    CLIENT_WRONG("client.integrity", CLEAR),
    CLIENT_SILENT("client.integrity", WEAK),
}

private const val STRONG = 10
private const val CLEAR = 5
private const val WEAK = 2

class Suspicions(
    dataSource: DataSource,
    private val config: Configuration,
    private val write: (Runnable) -> Unit,
    private val now: () -> Int,
) : SuspicionLog {
    private val sql = Sql(dataSource)
    private val scores = ConcurrentHashMap<Int, Score>()
    private val refreshing = ConcurrentHashMap.newKeySet<Int>()

    override fun record(
        playerId: Int?,
        address: String?,
        device: String?,
        kind: Suspicion,
        detail: String,
    ) {
        val at = now()
        log.info("suspicious: {} player {} from {}: {}", kind.key, playerId ?: "-", address ?: "-", detail)
        if (playerId != null) scores.computeIfPresent(playerId) { _, score -> score.plus(kind.weight) }
        write(
            Runnable {
                runCatching {
                    sql.write(INSERT) {
                        value(playerId)
                        string(address.orEmpty().take(ADDRESS_LENGTH))
                        string(device.orEmpty().take(DEVICE_LENGTH))
                        string(kind.key)
                        int(kind.weight)
                        string(detail.take(DETAIL_LENGTH))
                        int(at)
                    }
                }.onFailure { log.warn("suspicion not saved: {}", it.message) }
            },
        )
    }

    override fun muted(playerId: Int): Boolean {
        val needed = config.getInt(MUTE_SCORE_KEY, MUTE_SCORE)
        if (needed <= 0) return false
        val at = now()
        val held = scores[playerId]
        if ((held == null || at - held.readAt >= REREAD_SECONDS) && refreshing.add(playerId)) {
            write(
                Runnable {
                    try {
                        scores[playerId] = Score(read(playerId, at), at)
                    } finally {
                        refreshing.remove(playerId)
                    }
                },
            )
        }
        return (held?.total ?: 0) >= needed
    }

    override fun forget(playerId: Int) {
        scores.remove(playerId)
    }

    private fun read(
        playerId: Int,
        at: Int,
    ): Int {
        val since = at - config.getInt(WINDOW_KEY, WINDOW_HOURS) * SECONDS_PER_HOUR
        return runCatching { sql.select(SCORE, playerId, since).first { it.getInt(1) } ?: 0 }.getOrDefault(0)
    }

    private data class Score(
        val total: Int,
        val readAt: Int,
    ) {
        fun plus(weight: Int): Score = copy(total = total + weight)
    }

    companion object {
        const val MUTE_SCORE_KEY = "security.shadow_mute.score"
        const val WINDOW_KEY = "security.suspicion.window_hours"
        const val MUTE_SCORE = 10
        const val WINDOW_HOURS = 24
        private const val REREAD_SECONDS = 60
        private const val SECONDS_PER_HOUR = 3600
        private const val ADDRESS_LENGTH = 45
        private const val DEVICE_LENGTH = 120
        private const val DETAIL_LENGTH = 255
        private const val INSERT =
            "INSERT INTO suspicions (player_id, address, device, kind, weight, detail, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        private const val SCORE =
            "SELECT COALESCE(SUM(weight), 0) FROM suspicions WHERE player_id = ? AND created_at >= ?"
        private val log: Logger = LoggerFactory.getLogger(Suspicions::class.java)
    }
}
