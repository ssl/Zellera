package zel.wired.effect.user

import zel.game.items.Item
import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.unit.RoomUnit
import zel.util.MILLIS_PER_SECOND

@Suppress("TooManyFunctions") // the one seam between these boxes and the areas around them
interface UserEffectHost {
    fun kickOut(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun kickException(
        scope: RoomScope,
        playerId: Int,
    ): String? = null

    fun mute(
        scope: RoomScope,
        playerId: Int,
        minutes: Int,
    ) {}

    fun wake(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun linkerPair(furniId: Int): LinkerPair? = null

    /** The player holds a ban that is still running in [roomId]. */
    fun bannedFrom(
        roomId: Int,
        playerId: Int,
    ): Boolean = false

    fun teleportLandMillis(): Long = Teleports.LAND_MILLIS

    fun maxCopies(): Int = CopyAllowance.MAX_LIVE

    /**
     * Moves the player into [roomId] on the server, past any door mode, arriving on [furniId]; false when gone.
     * [afterwards] runs off the room's thread once the entry is answered, whatever the answer.
     */
    fun travel(
        scope: RoomScope,
        playerId: Int,
        roomId: Int,
        furniId: Int,
        afterwards: () -> Unit,
    ): Boolean = false

    /** In one transaction; the new id, or 0. */
    fun placeCopy(
        ownerId: Int,
        definition: Item,
        state: String,
        place: CopyPlace,
    ): Int = 0

    fun behaviour(type: String): FurniBehaviour = zel.room.furni.PlainFurni

    fun copiesOf(roomId: Int): Set<Int> = emptySet()

    /** On the persistence executor. */
    fun destroyCopies(rowIds: List<Int>) {}

    fun giveReward(
        playerId: Int,
        reward: StaffReward,
    ): Boolean = false

    fun ownsReward(
        playerId: Int,
        reward: StaffReward,
    ): Boolean = false

    /** Newest first. */
    fun rewardsGiven(
        furniId: Int,
        playerId: Int,
    ): List<GivenReward> = emptyList()

    /** Rewards still being stored included. */
    fun rewardCount(furniId: Int): Int = 0

    /** Reads the stored reward counts of these boxes while their room loads, before its first tick. */
    fun loadRewardCounts(furniIds: Collection<Int>) {}

    /** Drops the cached reward counts of these boxes; called when their room unloads. */
    fun forgetRewardCounts(furniIds: Collection<Int>) {}

    /** Counts the reward now and stores it on the persistence executor. */
    fun noteReward(
        furniId: Int,
        playerId: Int,
        reward: GivenReward,
    ) {}

    fun unixSeconds(): Int = (System.currentTimeMillis() / MILLIS_PER_SECOND).toInt()

    companion object {
        val None: UserEffectHost = object : UserEffectHost {}
    }
}
