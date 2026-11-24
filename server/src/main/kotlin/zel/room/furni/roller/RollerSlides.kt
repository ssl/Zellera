package zel.room.furni.roller

import zel.game.items.FurniHeights
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.SlideObjectBundleOut
import zel.room.map.Point

object RollerSlides {
    private const val SLIDE = 2

    @Suppress("LongParameterList") // one parameter per field of the message
    fun unit(
        from: Point,
        to: Point,
        rollerId: Int,
        unitIndex: Int,
        fromZ: Double,
        toZ: Double,
    ): SlideObjectBundleOut =
        SlideObjectBundleOut(
            from.x,
            from.y,
            to.x,
            to.y,
            emptyList(),
            rollerId,
            PacketWriter()
                .int(SLIDE)
                .int(unitIndex)
                .string(FurniHeights.formatZ(fromZ))
                .string(FurniHeights.formatZ(toZ))
                .toByteArray(),
        )

    @Suppress("LongParameterList") // one parameter per field of the message
    fun furni(
        from: Point,
        to: Point,
        furniId: Int,
        fromZ: Double,
        toZ: Double,
        rollerId: Int,
    ): SlideObjectBundleOut =
        SlideObjectBundleOut(from.x, from.y, to.x, to.y, listOf(item(furniId, fromZ, toZ)), rollerId, ByteArray(0))

    fun item(
        furniId: Int,
        fromZ: Double,
        toZ: Double,
    ): SlideObjectBundleOut.List1Item =
        SlideObjectBundleOut.List1Item(furniId, FurniHeights.formatZ(fromZ), FurniHeights.formatZ(toZ))

    /**
     * One roller's pass in as few bundles as the message allows: its furni, top first, ride in one bundle with the
     * last unit slide (the rider, not its horse); further unit slides go first on their own. A doormat and the user
     * standing on it roll in one SlideObjectBundle; several furni of one roller share it too.
     */
    fun bundles(
        from: Point,
        to: Point,
        rollerId: Int,
        units: List<SlideObjectBundleOut>,
        items: List<SlideObjectBundleOut.List1Item>,
    ): List<SlideObjectBundleOut> {
        if (items.isEmpty()) return units
        val unitPart = units.lastOrNull()?.rest ?: ByteArray(0)
        val carried = SlideObjectBundleOut(from.x, from.y, to.x, to.y, items, rollerId, unitPart)
        return units.dropLast(1) + carried
    }
}
