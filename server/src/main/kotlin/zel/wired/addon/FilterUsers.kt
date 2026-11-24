package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import kotlin.random.Random

class FilterUsers(
    furniId: Int,
    settings: BoxSettings,
    amounts: FilterAmounts,
    random: Random,
) : AmountFilter(furniId, settings, amounts, random) {
    override fun furni(
        firing: Firing,
        furniIds: List<Int>,
    ): List<Int> = furniIds

    override fun units(
        firing: Firing,
        unitIndexes: List<Int>,
    ): List<Int> = cut(firing, unitIndexes)
}
