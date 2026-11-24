package zel.wired.box.pattern

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class UserCountIn(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val a = (settings.ints.getOrNull(0) ?: FRESH_MIN).coerceIn(0, LIMIT)
        val b = (settings.ints.getOrNull(1) ?: FRESH_MAX).coerceIn(0, LIMIT)
        val people =
            firing.scope.room.units
                .all()
                .count { it.kind is UnitKind.Player }
        return people in a..b
    }

    companion object {
        const val FRESH_MIN = 1
        const val FRESH_MAX = 50
        private const val LIMIT = 1000
    }
}
