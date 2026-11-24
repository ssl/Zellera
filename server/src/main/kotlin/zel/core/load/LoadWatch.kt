package zel.core.load

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.core.load.ServerGauges.Meter
import zel.core.threading.GameThreads
import zel.util.NANOS_PER_MILLI
import java.util.EnumMap
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Says whether the server takes another login. Every meter is smoothed over time, so a burst passes unnoticed and a
 * lasting load shows within a few smoothing times; logins close when one smoothed meter reaches the refuse share
 * and open again when all are under the resume share. A database that is down closes them at once: no login can
 * succeed without it.
 */
class LoadWatch(
    private val settings: Settings,
    private val gauges: () -> LoadSample,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    data class Settings(
        val refusesLogins: Boolean = false,
        val refuseShare: Double = REFUSE_PERCENT / PERCENT,
        val resumeShare: Double = RESUME_PERCENT / PERCENT,
        val smoothingSeconds: Int = SMOOTHING_SECONDS,
        val schedulerLateMillis: Int = SCHEDULER_LATE_MILLIS,
    ) {
        init {
            require(refuseShare > 0.0 && refuseShare <= 1.0) { "logins are refused at a share above 0, up to 1" }
            require(resumeShare >= 0.0 && resumeShare < refuseShare) { "logins resume under the refuse share" }
            require(smoothingSeconds > 0 && schedulerLateMillis > 0) { "a time must be positive" }
        }

        companion object {
            const val REFUSE_PERCENT = 97
            const val RESUME_PERCENT = 90
            const val SMOOTHING_SECONDS = 10
            const val SCHEDULER_LATE_MILLIS = 10_000
            private const val FULL_PERCENT = 100
            private const val PERCENT = 100.0

            /** A value out of range takes its default; a resume share not under the refuse share keeps their gap. */
            fun from(configuration: Configuration): Settings {
                fun positive(
                    key: String,
                    fallback: Int,
                ) = configuration.getInt("server.load.$key", fallback).takeIf { it > 0 } ?: fallback

                val refuse = positive("refuse.percent", REFUSE_PERCENT).takeIf { it <= FULL_PERCENT } ?: REFUSE_PERCENT
                val resume =
                    configuration.getInt("server.load.resume.percent", RESUME_PERCENT).takeIf { it in 0 until refuse }
                        ?: (refuse - (REFUSE_PERCENT - RESUME_PERCENT)).coerceAtLeast(0)
                return Settings(
                    configuration.getBoolean("server.load.refuses_logins", false),
                    refuse / PERCENT,
                    resume / PERCENT,
                    positive("smoothing.seconds", SMOOTHING_SECONDS),
                    positive("scheduler.late.ms", SCHEDULER_LATE_MILLIS),
                )
            }
        }
    }

    private val smoothingNanos = TimeUnit.SECONDS.toNanos(settings.smoothingSeconds.toLong()).toDouble()

    // All guarded by this: the scheduler samples every second, and a login that finds the last sample old takes one.
    private val smoothed = EnumMap<Meter, Double>(Meter::class.java)
    private var sampledAt: Long? = null
    private var high = false

    /** True while the load is too high for another login, whether logins are refused for it or not. */
    val isHigh: Boolean
        @Synchronized get() = high

    fun admitsLogin(): Boolean {
        sampleIfDue()
        return !(settings.refusesLogins && isHigh)
    }

    fun start(threading: GameThreads) {
        threading.run(
            {
                sampleIfDue()
                start(threading)
            },
            SAMPLE_GAP_NANOS / NANOS_PER_MILLI,
        )
    }

    @Synchronized
    fun sampleIfDue() {
        val now = nanoTime()
        val gap = sampledAt?.let { now - it }
        if (gap != null && gap < SAMPLE_GAP_NANOS) return
        sampledAt = now
        val sample = read() ?: return
        val weight = if (gap == null) 1.0 else 1.0 - exp(-gap.toDouble() / smoothingNanos)
        smoothed.keys.retainAll(sample.shares.keys)
        sample.shares.forEach { (meter, share) ->
            smoothed.merge(meter, share) { before, _ -> before + weight * (share - before) }
        }
        decide(sample.databaseDown)
    }

    @Suppress("TooGenericExceptionCaught") // gauges that throw say nothing about the load
    private fun read(): LoadSample? =
        try {
            gauges()
        } catch (unreadable: RuntimeException) {
            log.warn("the load could not be read: {}", unreadable.toString())
            null
        }

    private fun decide(databaseDown: Boolean) {
        val fullest = smoothed.maxByOrNull { it.value }
        val peak = fullest?.value ?: 0.0
        val next = databaseDown || peak >= (if (high) settings.resumeShare else settings.refuseShare)
        if (next == high) return
        high = next
        val effect = if (settings.refusesLogins) "new logins are turned away" else "new logins are still taken"
        when {
            !next -> log.info("load back under {}%: logins are taken as usual", percent(settings.resumeShare))
            databaseDown -> log.warn("the database is down: {}", effect)
            else -> log.warn("load high, {} at {}%: {}", fullest?.key?.label, percent(peak), effect)
        }
    }

    private fun percent(share: Double): Int = (share * FULL).roundToInt()

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LoadWatch::class.java)
        val SAMPLE_GAP_NANOS = TimeUnit.SECONDS.toNanos(1)
        const val FULL = 100
    }
}
