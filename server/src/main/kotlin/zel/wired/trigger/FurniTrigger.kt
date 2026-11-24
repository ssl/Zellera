package zel.wired.trigger

import zel.wired.box.TriggerBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import kotlin.math.absoluteValue

interface FurniTrigger : TriggerBox {
    val furniSource: Int get() = settings.furniSources.firstOrNull() ?: FurniSource.PICKED

    fun listens(furniId: Int): Boolean =
        furniSource == FurniSource.SELECTION || settings.picks.any { it.absoluteValue == furniId }

    override fun answers(firing: Firing): Boolean =
        furniSource != FurniSource.SELECTION || firing.triggeringFurni in firing.selection.furniIds
}
