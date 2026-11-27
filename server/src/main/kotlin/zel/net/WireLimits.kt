package zel.net

import zel.core.Configuration
import zel.core.rules.rules
import zel.protocol.generated.RenderRoomInCodec
import zel.protocol.generated.RenderRoomThumbnailInCodec

/** The defaults fit the largest message, a 320 x 320 RGBA camera upload. */
data class WireLimits(
    /** As the length field counts it: message id plus body. */
    val maxFrameBytes: Int = rules.limits.frameBytes,
    /** The largest frame of any message but [largeFrames], counted as [maxFrameBytes] is. */
    val ordinaryFrameBytes: Int = DEFAULT_ORDINARY_FRAME_BYTES.coerceAtMost(maxFrameBytes),
    /** The camera uploads, the only messages that carry a picture; they may take [maxFrameBytes]. */
    val largeFrames: Set<String> = setOf(RenderRoomInCodec.name, RenderRoomThumbnailInCodec.name),
    /** Room for the largest frame and as much again of what a client sent along in the same socket message. */
    val maxSocketMessageBytes: Int = maxFrameBytes * 2,
    /** Seconds without traffic before a ping, and seconds then waited for any reply. */
    val pingGapSeconds: Int = DEFAULT_PING_GAP_SECONDS,
    val pingTimeoutSeconds: Int = DEFAULT_PING_TIMEOUT_SECONDS,
    val loginSeconds: Int = DEFAULT_LOGIN_SECONDS,
    val upgradeTimeoutSeconds: Int = DEFAULT_UPGRADE_TIMEOUT_SECONDS,
    val pendingPerProxy: Int = DEFAULT_PENDING_PER_PROXY,
    val rateWindowMillis: Long = 1_000,
    val perMessagePerWindow: Int = rules.limits.messagesPerKindPerSecond,
    val totalPerWindow: Int = rules.limits.messagesPerSecond,
    val upgradeBytes: Int = DEFAULT_UPGRADE_BYTES,
    val bridgeBytes: Int = DEFAULT_BRIDGE_BYTES,
) {
    companion object {
        const val DEFAULT_PING_GAP_SECONDS = 60
        const val DEFAULT_PING_TIMEOUT_SECONDS = 120
        const val DEFAULT_LOGIN_SECONDS = 120
        const val DEFAULT_UPGRADE_TIMEOUT_SECONDS = 10
        const val DEFAULT_PENDING_PER_PROXY = 256
        const val DEFAULT_UPGRADE_BYTES = 16 * 1024
        const val DEFAULT_BRIDGE_BYTES = 256 * 1024

        /**
         * 128 KiB: two strings of the longest length a string field can declare (65,535 bytes), or 32,768 ids in a
         * list. Every message but the camera upload is normally under 100 bytes.
         */
        const val DEFAULT_ORDINARY_FRAME_BYTES = 128 * 1024

        /** A value that is not positive takes its default. */
        fun from(configuration: Configuration): WireLimits {
            fun positive(
                key: String,
                fallback: Int,
            ) = configuration.getInt("network.$key", fallback).takeIf { it > 0 } ?: fallback

            return WireLimits(
                pingGapSeconds = positive("ping.gap.seconds", DEFAULT_PING_GAP_SECONDS),
                pingTimeoutSeconds = positive("ping.timeout.seconds", DEFAULT_PING_TIMEOUT_SECONDS),
                loginSeconds = positive("login.timeout.seconds", DEFAULT_LOGIN_SECONDS),
                upgradeTimeoutSeconds = positive("upgrade.timeout.seconds", DEFAULT_UPGRADE_TIMEOUT_SECONDS),
                pendingPerProxy =
                    configuration
                        .getInt("network.connections.pending_per_proxy", DEFAULT_PENDING_PER_PROXY)
                        .takeIf { it >= 0 } ?: DEFAULT_PENDING_PER_PROXY,
                upgradeBytes = positive("upgrade.max.bytes", DEFAULT_UPGRADE_BYTES),
                bridgeBytes = positive("bridge.request.max.bytes", DEFAULT_BRIDGE_BYTES),
            )
        }
    }
}
