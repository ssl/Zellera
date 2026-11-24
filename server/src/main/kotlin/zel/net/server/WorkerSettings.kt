package zel.net.server

import zel.core.Configuration
import zel.core.rules.rules

/** Packet and bridge pool sizes; non-positive values take the default. Backlogs count waiting plus running. */
data class WorkerSettings(
    val pausesReading: Boolean = true,
    val packetThreads: Int = defaultPacketThreads(),
    val packetBacklog: Int = packetThreads * PACKETS_PER_THREAD,
    val perConnection: Int = defaultPerConnection(),
    val bridgeThreads: Int = BRIDGE_THREADS,
    val bridgeBacklog: Int = bridgeThreads * REQUESTS_PER_THREAD,
    val packetBacklogBytes: Long = packetThreads * BYTES_PER_THREAD,
    val perConnectionBytes: Long = defaultPerConnectionBytes().coerceAtMost(packetBacklogBytes),
) {
    fun packetPool(): WorkerPool =
        WorkerPool("packets", packetThreads, packetBacklog, pausesReading, packetBacklogBytes)

    fun bridgePool(): WorkerPool = WorkerPool("bridge", bridgeThreads, bridgeBacklog, limited = true)

    companion object {
        const val PACKETS_PER_THREAD = 250
        const val BRIDGE_THREADS = 8
        const val REQUESTS_PER_THREAD = 32
        const val SECONDS_PER_CONNECTION = 4

        /** 4 MiB: ten of the largest frames for every packet thread. */
        const val BYTES_PER_THREAD = 4L * 1024 * 1024

        /** A connection may hold this many of the largest frames. */
        const val LARGEST_FRAMES_PER_CONNECTION = 2
        private const val LEAST_PACKET_THREADS = 16

        fun from(configuration: Configuration): WorkerSettings {
            fun positive(key: String): Int? = configuration.getInt(key, 0).takeIf { it > 0 }

            val packetThreads = positive("network.packets.threads") ?: defaultPacketThreads()
            val packetBacklog = positive("network.packets.backlog.max") ?: (packetThreads * PACKETS_PER_THREAD)
            val packetBacklogBytes =
                positive("network.packets.backlog.max.bytes")?.toLong() ?: (packetThreads * BYTES_PER_THREAD)
            val bridgeThreads = positive("network.bridge.threads") ?: BRIDGE_THREADS
            return WorkerSettings(
                pausesReading = configuration.getBoolean("network.packets.backlog.pauses_reading", true),
                packetThreads = packetThreads,
                packetBacklog = packetBacklog,
                perConnection =
                    (positive("network.packets.backlog.per_connection") ?: defaultPerConnection())
                        .coerceAtMost(packetBacklog),
                bridgeThreads = bridgeThreads,
                bridgeBacklog = positive("network.bridge.backlog.max") ?: (bridgeThreads * REQUESTS_PER_THREAD),
                packetBacklogBytes = packetBacklogBytes,
                perConnectionBytes =
                    (positive("network.packets.backlog.per_connection.bytes")?.toLong() ?: defaultPerConnectionBytes())
                        .coerceAtMost(packetBacklogBytes),
            )
        }

        fun defaultPacketThreads(): Int = maxOf(LEAST_PACKET_THREADS, Runtime.getRuntime().availableProcessors() * 2)

        /** A few seconds of the messages a connection may send. */
        fun defaultPerConnection(): Int = (rules.limits.messagesPerSecond * SECONDS_PER_CONNECTION).coerceAtLeast(1)

        /** Two of the largest frames: one camera upload waiting while another runs. */
        fun defaultPerConnectionBytes(): Long = rules.limits.frameBytes.toLong() * LARGEST_FRAMES_PER_CONNECTION
    }
}
