package zel.room.rights

import zel.protocol.generated.RaidProtectionSettingsOut
import zel.protocol.generated.RaidProtectionSettingsOutCodec
import zel.protocol.generated.RaidProtectionSettingsResultOut
import zel.protocol.generated.RaidProtectionSettingsResultOutCodec

class RaidProtection(
    private val store: RightsStore,
    private val active: (Int) -> Boolean = { false },
    private val lastRaid: (Int) -> Int = { 0 },
    private val saved: (Int, RaidSettings) -> Unit = { _, _ -> },
) {
    fun get(
        roomId: Int,
        playerRoom: Int?,
        level: Int,
        reply: Reply,
    ) {
        if (roomId == 0 || roomId != playerRoom) return result(roomId, NOT_HERE, RaidSettings.DEFAULT, reply)
        val stored = store.raid(roomId) ?: RaidSettings.DEFAULT
        if (level < MANAGE_LEVEL) return result(roomId, NO_RIGHTS, stored, reply)
        reply.send(RaidProtectionSettingsOutCodec, settings(roomId, stored))
    }

    fun save(
        roomId: Int,
        playerRoom: Int?,
        level: Int,
        wanted: RaidSettings,
        confirmed: Boolean,
        reply: Reply,
    ) {
        if (roomId == 0 || roomId != playerRoom) return result(roomId, NOT_HERE, RaidSettings.DEFAULT, reply)
        val stored = store.raid(roomId) ?: RaidSettings.DEFAULT
        val code =
            when {
                level < MANAGE_LEVEL -> NO_RIGHTS
                !wanted.valid() -> INVALID
                wanted.enabled && !stored.enabled && !confirmed -> CONFIRM
                else -> SAVED
            }
        if (code == SAVED) {
            store.saveRaid(roomId, wanted)
            saved(roomId, wanted)
        }
        result(roomId, code, if (code == SAVED) wanted else stored, reply)
    }

    private fun result(
        roomId: Int,
        code: Int,
        s: RaidSettings,
        reply: Reply,
    ) = reply.send(
        RaidProtectionSettingsResultOutCodec,
        RaidProtectionSettingsResultOut(
            roomId,
            code,
            s.enabled,
            s.sensitivity,
            s.actionType,
            s.banSeconds,
            s.guardEnabled,
            s.guardSeconds,
            s.guardSensitivity,
            active(roomId),
            lastRaid(roomId),
        ),
    )

    private fun settings(
        roomId: Int,
        s: RaidSettings,
    ) = RaidProtectionSettingsOut(
        roomId,
        s.enabled,
        s.sensitivity,
        s.actionType,
        s.banSeconds,
        s.guardEnabled,
        s.guardSeconds,
        s.guardSensitivity,
        active(roomId),
        lastRaid(roomId),
    )

    companion object {
        const val SAVED = 0
        const val NOT_HERE = 2
        const val NO_RIGHTS = 3
        const val INVALID = 4
        const val CONFIRM = 5
        private const val MANAGE_LEVEL = 1
    }
}
