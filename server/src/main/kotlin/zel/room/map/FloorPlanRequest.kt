package zel.room.map

import zel.protocol.generated.MalformedPacketException
import zel.protocol.generated.PacketReader

data class FloorPlanRequest(
    val map: String,
    val door: Door,
    val wallThickness: Int,
    val floorThickness: Int,
    val fixedWallHeight: Int?,
) {
    companion object {
        private const val INT_BYTES = 4

        private const val REQUIRED_BYTES = INT_BYTES * 5

        fun read(body: ByteArray): FloorPlanRequest? =
            try {
                val r = PacketReader(body)
                val map = r.string()
                if (r.remaining < REQUIRED_BYTES) {
                    null
                } else {
                    FloorPlanRequest(
                        map = map,
                        door = Door(Point(r.int(), r.int()), r.int()),
                        wallThickness = r.int(),
                        floorThickness = r.int(),
                        fixedWallHeight = if (r.remaining >= INT_BYTES) r.int() else null,
                    )
                }
            } catch (_: MalformedPacketException) {
                null
            } catch (_: java.nio.BufferUnderflowException) {
                null
            }
    }
}
