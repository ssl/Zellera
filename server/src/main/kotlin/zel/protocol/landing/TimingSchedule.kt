package zel.protocol.landing

import zel.util.GameZone
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** `yyyy-MM-dd HH:mm,code;...`: the entry due last decides the current code. */
internal object TimingSchedule {
    val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private const val LONGEST = 1024

    /** Empty for none, or for text too long to be a schedule. */
    fun dueCode(
        schedule: String?,
        now: LocalDateTime = LocalDateTime.now(GameZone.clock),
    ): String {
        if (schedule == null || schedule.length > LONGEST) return ""
        var latest: Pair<LocalDateTime, String>? = null
        for (entry in schedule.split(';')) {
            val (at, code) = slot(entry) ?: continue
            if (at <= now && (latest == null || at > latest.first)) latest = at to code
        }
        return latest?.second.orEmpty()
    }

    private fun slot(entry: String): Pair<LocalDateTime, String>? {
        val comma = entry.indexOf(',')
        if (comma < 0) return null
        val stamp = entry.substring(0, comma).trim()
        val at = runCatching { LocalDateTime.parse(stamp, STAMP) }.getOrNull() ?: return null
        return at to entry.substring(comma + 1)
    }
}
