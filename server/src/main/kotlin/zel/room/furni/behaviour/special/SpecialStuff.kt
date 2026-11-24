package zel.room.furni.behaviour.special

import zel.protocol.generated.StuffData
import zel.room.furni.RoomFurni

object SpecialStuff {
    /** Format bit that appends the limited edition numbers. */
    private const val LIMITED_FLAG = 0x100
    const val LEGACY = 0
    const val MAP = 1
    const val STRINGS = 2
    const val INTS = 5
    const val CRACKABLE = 7

    fun of(
        furni: RoomFurni,
        format: Int,
        value: StuffData.Value,
    ): StuffData = of(format, value, edition(furni.limitedData))

    fun of(
        format: Int,
        value: StuffData.Value,
        limited: StuffData.Limited?,
    ): StuffData = StuffData(if (limited == null) format else format or LIMITED_FLAG, value, limited)

    fun strings(
        furni: RoomFurni,
        values: List<String>,
    ): StuffData = of(furni, STRINGS, StuffData.Value.StringArray(values))

    fun map(
        furni: RoomFurni,
        values: Map<String, String>,
    ): StuffData = of(furni, MAP, StuffData.Value.MapData(values.toList()))

    fun legacy(
        furni: RoomFurni,
        state: String,
    ): StuffData = of(furni, LEGACY, StuffData.Value.Legacy(state))

    /** [data] is "total:number"; a furni with both 0 is no limited edition. */
    private fun edition(data: String): StuffData.Limited? {
        val total = data.substringBefore(':').toIntOrNull() ?: 0
        val number = data.substringAfter(':', "").toIntOrNull() ?: 0
        return if (number == 0 && total == 0) null else StuffData.Limited(number, total)
    }
}
