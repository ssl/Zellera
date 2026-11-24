package zel.room.port

import zel.room.chat.ChatBubbleStyle

@Suppress("TooManyFunctions") // one per room command
interface RoomCommands {
    fun rightsLevel(playerId: Int): Int

    fun changePosture(
        playerId: Int,
        sit: Boolean,
    )

    fun giveEffect(
        playerId: Int,
        effectId: Int,
        seconds: Int = NO_END,
    )

    fun effectOf(playerId: Int): Int

    fun whisper(
        playerId: Int,
        text: String,
        bubble: ChatBubbleStyle,
    )

    fun isIdle(playerId: Int): Boolean

    fun isSitting(playerId: Int): Boolean

    fun pickAll(playerId: Int)

    /** Hands every furni the room owner does not own back to its owner. */
    fun ejectGuestFurni(playerId: Int)

    fun pickBuildPass(playerId: Int)

    fun ejectPets(playerId: Int)

    fun resetScores(playerId: Int)

    fun playerNamed(
        playerId: Int,
        name: String,
    ): Int?

    fun staffKick(
        staffId: Int,
        targetId: Int,
    )

    fun staffMute(
        staffId: Int,
        targetId: Int,
        minutes: Int,
    )

    companion object {
        const val NO_END = -1

        val None: RoomCommands =
            object : RoomCommands {
                override fun rightsLevel(playerId: Int) = 0

                override fun changePosture(
                    playerId: Int,
                    sit: Boolean,
                ) = Unit

                override fun giveEffect(
                    playerId: Int,
                    effectId: Int,
                    seconds: Int,
                ) = Unit

                override fun effectOf(playerId: Int) = 0

                override fun whisper(
                    playerId: Int,
                    text: String,
                    bubble: ChatBubbleStyle,
                ) = Unit

                override fun isIdle(playerId: Int) = false

                override fun isSitting(playerId: Int) = false

                override fun pickAll(playerId: Int) = Unit

                override fun ejectGuestFurni(playerId: Int) = Unit

                override fun pickBuildPass(playerId: Int) = Unit

                override fun ejectPets(playerId: Int) = Unit

                override fun resetScores(playerId: Int) = Unit

                override fun playerNamed(
                    playerId: Int,
                    name: String,
                ): Int? = null

                override fun staffKick(
                    staffId: Int,
                    targetId: Int,
                ) = Unit

                override fun staffMute(
                    staffId: Int,
                    targetId: Int,
                    minutes: Int,
                ) = Unit
            }
    }
}
