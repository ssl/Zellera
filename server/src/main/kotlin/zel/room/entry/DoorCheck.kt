package zel.room.entry

import zel.room.DoorMode

object DoorCheck {
    private val PASS_DOOR = setOf(RIGHTS, GROUP_ADMIN, OWNER, STAFF)

    fun answer(
        door: DoorMode,
        passwordFits: Boolean,
        visitor: Visitor,
        playersInside: Int,
        maxUsers: Int,
        bellAnswered: Boolean,
        buildPassLocked: Boolean = false,
        arrivesPastDoor: Boolean = false,
    ): DoorAnswer {
        val banned = visitor.banned && !visitor.entersAnyRoom
        // A bell lets a banned visitor ring; one arriving past the door has no bell to ring at.
        if (banned && (door != DoorMode.Doorbell || bellAnswered || arrivesPastDoor)) return DoorAnswer.Banned
        if (buildPassLocked && !bellAnswered && !passesBuildPassLock(visitor)) {
            return DoorAnswer.BuildPassLocked
        }
        if (banned) return DoorAnswer.Ring
        if (!bellAnswered && !walksPast(door, visitor, arrivesPastDoor)) {
            doorAnswer(door, passwordFits)?.let { return it }
        }
        val skipsFull = visitor.level >= RIGHTS || visitor.entersFullRooms || visitor.entersAnyRoom
        return if (!skipsFull && playersInside >= maxUsers) DoorAnswer.Queue else DoorAnswer.In
    }

    fun passesBuildPassLock(visitor: Visitor): Boolean = visitor.entersAnyRoom || visitor.level >= OWNER

    /** Whether the visitor's typed password decides the answer, so that only then it is compared. */
    fun asksPassword(
        door: DoorMode,
        visitor: Visitor,
        buildPassLocked: Boolean = false,
        arrivesPastDoor: Boolean = false,
    ): Boolean {
        if (door !is DoorMode.Password || (visitor.banned && !visitor.entersAnyRoom)) return false
        if (buildPassLocked && !passesBuildPassLock(visitor)) return false
        return !walksPast(door, visitor, arrivesPastDoor)
    }

    private fun walksPast(
        door: DoorMode,
        visitor: Visitor,
        arrivesPastDoor: Boolean,
    ): Boolean {
        val skipsBell = visitor.groupMember && door == DoorMode.Doorbell
        return arrivesPastDoor || visitor.entersAnyRoom || visitor.level in PASS_DOOR || skipsBell
    }

    private fun doorAnswer(
        door: DoorMode,
        passwordFits: Boolean,
    ): DoorAnswer? =
        when (door) {
            DoorMode.Open -> null
            DoorMode.Doorbell -> DoorAnswer.Ring
            is DoorMode.Password -> if (passwordFits) null else DoorAnswer.WrongPassword
            DoorMode.Invisible -> DoorAnswer.Invisible
        }

    const val RIGHTS = 1
    const val GROUP_MEMBER = 2
    const val GROUP_ADMIN = 3
    const val OWNER = 4
    const val STAFF = 5
}
