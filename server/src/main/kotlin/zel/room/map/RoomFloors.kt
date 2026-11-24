package zel.room.map

import zel.room.FloorSource
import zel.room.RoomInfo
import java.util.concurrent.atomic.AtomicReference

class RoomFloors(
    private val data: FloorData,
) : FloorSource {
    private val models = AtomicReference<Map<String, RoomModel>?>(null)

    fun model(name: String): RoomModel? = modelsByName()[name.lowercase()]

    override fun floorOf(info: RoomInfo): FloorPlan? {
        val model = model(info.modelName) ?: return null
        val settings = data.settings(info.id) ?: return null
        val custom = if (settings.customFloor) data.customFloor(info.id) else null
        val map = custom?.map ?: model.map
        val door = custom?.door ?: model.door
        val grid = FloorMapText.parseStored(map)
        return FloorPlan(grid.width, grid.height, grid.heights, door, model.smallScale, settings.fixedWallHeight)
    }

    private fun modelsByName(): Map<String, RoomModel> = models.get() ?: read().also { models.compareAndSet(null, it) }

    private fun read(): Map<String, RoomModel> = data.models().associateBy { it.name.lowercase() }
}
