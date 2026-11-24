package zel.wired.variable

data class SharedToken(
    val target: Int,
    val roomId: Int,
    val boxId: Int,
) {
    val text: String get() = "$PREFIX${if (target == VariableTarget.USER) USER_CODE else ROOM_CODE}:$roomId:$boxId"

    companion object {
        private const val PREFIX = "shared:"
        private const val USER_CODE = 0
        private const val ROOM_CODE = 3
        private const val PARTS = 4

        fun parse(text: String): SharedToken? {
            val parts = text.split(':')
            if (parts.size != PARTS || parts[0] != PREFIX.trimEnd(':')) return null
            val target =
                when (parts[1].toIntOrNull()) {
                    USER_CODE -> VariableTarget.USER
                    ROOM_CODE -> VariableTarget.GLOBAL
                    else -> return null
                }
            val roomId = parts[2].toIntOrNull() ?: return null
            val boxId = parts[PARTS - 1].toIntOrNull() ?: return null
            return SharedToken(target, roomId, boxId)
        }
    }
}
