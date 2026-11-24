package zel.util

import zel.core.Configuration
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/**
 * The game's local time: the `game.time_zone` setting (ZEL_TIME_ZONE in config/zellera.env, else its settings row),
 * else the system's. Dates players see and local-day rules use it;
 * the clocks the client expects in UTC (daily tasks, quest days, wire stamps) stay UTC.
 */
object GameZone {
    private val log = LoggerFactory.getLogger(GameZone::class.java)

    const val SETTING = "game.time_zone"

    private val zone = AtomicReference<ZoneId>(ZoneId.systemDefault())

    val current: ZoneId get() = zone.get()

    /** Always in [current], also after a settings reload changes it. */
    val clock: Clock =
        object : Clock() {
            override fun getZone(): ZoneId = current

            override fun withZone(zone: ZoneId): Clock = system(zone)

            override fun instant(): Instant = Instant.now()
        }

    fun today(): LocalDate = LocalDate.now(current)

    /** Takes the setting now and after every settings reload. */
    fun follow(configuration: Configuration) {
        zone.set(of(configuration))
        configuration.onReload { zone.set(of(configuration)) }
    }

    fun of(configuration: Configuration): ZoneId {
        val name = configuration.getValue(SETTING, "").trim()
        if (name.isEmpty()) return ZoneId.systemDefault()
        return try {
            ZoneId.of(name)
        } catch (e: DateTimeException) {
            log.warn("{} '{}' is not a zone, using the system's: {}", SETTING, name, e.message)
            ZoneId.systemDefault()
        }
    }
}
