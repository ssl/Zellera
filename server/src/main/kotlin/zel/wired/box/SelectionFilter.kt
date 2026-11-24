package zel.wired.box

import zel.wired.firing.Firing

interface SelectionFilter : AddonBox {
    val rank: Int

    fun furni(
        firing: Firing,
        furniIds: List<Int>,
    ): List<Int>

    fun units(
        firing: Firing,
        unitIndexes: List<Int>,
    ): List<Int>
}
