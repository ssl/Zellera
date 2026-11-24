package zel.wired.condition

import zel.wired.box.BoxSettings

data class SnapshotFlags(
    val state: Boolean,
    val rotation: Boolean,
    val position: Boolean,
    val altitude: Boolean,
) {
    companion object {
        fun of(settings: BoxSettings): SnapshotFlags =
            SnapshotFlags(
                settings.intAt(STATE) == 1,
                settings.intAt(ROTATION) == 1,
                settings.intAt(POSITION) == 1,
                settings.intAt(ALTITUDE) == 1,
            )

        private const val STATE = 0
        private const val ROTATION = 1
        private const val POSITION = 2
        private const val ALTITUDE = 3
    }
}
