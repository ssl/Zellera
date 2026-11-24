package zel.core.load

import zel.core.threading.GameThreads
import zel.db.Database
import zel.db.DatabaseWriter
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

/** A meter that answers null is idle or stopped, except the database pool: null there means it is down. */
class ServerGauges(
    private val meters: Map<Meter, () -> Occupancy?>,
) {
    enum class Meter(
        val label: String,
    ) {
        DATABASE("database connections"),
        WRITERS("database write queue"),
        PACKETS("packets in work"),
        SCHEDULER("scheduler lateness"),
        HEAP("memory after collection"),
    }

    /** An unknown capacity reads as nothing in use. */
    data class Occupancy(
        val used: Long,
        val capacity: Long,
    ) {
        constructor(used: Int, capacity: Int) : this(used.toLong(), capacity.toLong())

        val share: Double get() = if (capacity <= 0) 0.0 else (used.toDouble() / capacity).coerceIn(0.0, 1.0)
    }

    private class Reading(
        val occupancy: Occupancy?,
        val failed: Boolean,
    )

    fun sample(): LoadSample {
        val readings = meters.mapValues { (_, meter) -> read(meter) }
        val shares =
            readings
                .filterValues { !it.failed }
                .mapValues { (meter, reading) -> reading.occupancy?.share ?: unreadShare(meter) }
        val pool = readings[Meter.DATABASE]
        return LoadSample(shares, databaseDown = pool != null && !pool.failed && pool.occupancy == null)
    }

    private fun unreadShare(meter: Meter): Double = if (meter == Meter.DATABASE) 1.0 else 0.0

    @Suppress("TooGenericExceptionCaught") // a throwing meter is left out of the sample
    private fun read(meter: () -> Occupancy?): Reading =
        try {
            Reading(meter(), failed = false)
        } catch (_: RuntimeException) {
            Reading(null, failed = true)
        }

    companion object {
        fun forServer(
            database: Database,
            writer: DatabaseWriter,
            threading: GameThreads,
            schedulerLateMillis: Int,
            packets: () -> Occupancy?,
        ): ServerGauges {
            val source = database.dataSource
            return ServerGauges(
                mapOf(
                    // threads waiting for a connection count as use: a pool with a queue is over full
                    Meter.DATABASE to {
                        source.hikariPoolMXBean
                            ?.takeUnless { source.isClosed }
                            ?.let { pool ->
                                val wanted = pool.activeConnections + pool.threadsAwaitingConnection
                                Occupancy(wanted, source.maximumPoolSize)
                            }
                    },
                    Meter.WRITERS to { writer.pressure().let { Occupancy(it.waiting, it.capacity) } },
                    Meter.PACKETS to packets,
                    Meter.SCHEDULER to { Occupancy(threading.lateness().toMillis(), schedulerLateMillis.toLong()) },
                    Meter.HEAP to ::liveHeap,
                ),
            )
        }

        /**
         * What the last collections left alive, of the most the heap may grow to. Plain heap use climbs to the top
         * between collections on a healthy server, so it says nothing.
         */
        private fun liveHeap(): Occupancy {
            val alive =
                ManagementFactory
                    .getMemoryPoolMXBeans()
                    .filter { it.type == MemoryType.HEAP }
                    .sumOf { it.collectionUsage?.used ?: 0L }
            return Occupancy(alive, Runtime.getRuntime().maxMemory())
        }
    }
}
