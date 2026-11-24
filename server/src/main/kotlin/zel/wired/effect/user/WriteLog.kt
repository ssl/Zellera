package zel.wired.effect.user

import zel.room.Room
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.store.RoomLog

class WriteLog(
    override val furniId: Int,
    override val settings: BoxSettings,
    override val negative: Boolean,
    private val texts: TextPlaceholders,
    private val roomLog: (Room, RoomLog.Line) -> Unit,
    private val wallClock: () -> Long = System::currentTimeMillis,
) : EffectBox {
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        val text = texts.fill(firing, null, settings.text).trim()
        if (text.isEmpty()) return
        val level = settings.ints.getOrElse(LEVEL) { DEFAULT_LEVEL }.coerceIn(0, MAX_LEVEL)
        roomLog(scope.room, RoomLog.Line(level, SOURCE_LOG_BOX, text.take(MAX_LINE), wallClock()))
    }

    companion object {
        const val MAX_TEXT = 200
        private const val LEVEL = 0
        private const val DEFAULT_LEVEL = 1
        internal const val MAX_LEVEL = 3
        private const val MAX_LINE = 1000
        private const val SOURCE_LOG_BOX = 1
    }
}
