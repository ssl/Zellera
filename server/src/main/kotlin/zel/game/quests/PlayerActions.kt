package zel.game.quests

import zel.game.players.Player
import java.util.concurrent.atomic.AtomicReference

/** The one entry point for reported player actions; they fall through until the quests module installs a [Counter]. */
object PlayerActions {
    interface Counter {
        fun goal(
            player: Player?,
            goal: CountedAction,
            amount: Int,
        )

        fun goal(
            playerId: Int,
            goal: CountedAction,
            amount: Int,
        )

        fun furni(
            player: Player?,
            action: FurniAction,
            furniName: String,
        )

        /** One action of [goal] whose [size] a target-bound quest measures. */
        fun reached(
            playerId: Int,
            goal: CountedAction,
            size: Int,
        )

        fun handItem(
            player: Player?,
            item: Int,
        )

        fun pet(
            player: Player?,
            petType: Int,
        )

        fun forget(playerId: Int)
    }

    private object Nobody : Counter {
        override fun goal(
            player: Player?,
            goal: CountedAction,
            amount: Int,
        ) = Unit

        override fun goal(
            playerId: Int,
            goal: CountedAction,
            amount: Int,
        ) = Unit

        override fun furni(
            player: Player?,
            action: FurniAction,
            furniName: String,
        ) = Unit

        override fun reached(
            playerId: Int,
            goal: CountedAction,
            size: Int,
        ) = Unit

        override fun handItem(
            player: Player?,
            item: Int,
        ) = Unit

        override fun pet(
            player: Player?,
            petType: Int,
        ) = Unit

        override fun forget(playerId: Int) = Unit
    }

    private val counting = AtomicReference<Counter>(Nobody)

    private val counter: Counter get() = counting.get()

    @JvmStatic
    fun countWith(counter: Counter) = counting.set(counter)

    /** A null [goal] (an action nothing counts) reports nothing. */
    @JvmStatic
    fun progress(
        player: Player?,
        goal: CountedAction?,
        amount: Int,
    ) {
        if (goal != null) counter.goal(player, goal, amount)
    }

    @JvmStatic
    fun progress(
        playerId: Int,
        goal: CountedAction?,
        amount: Int,
    ) {
        if (goal != null) counter.goal(playerId, goal, amount)
    }

    @JvmStatic
    fun reached(
        playerId: Int,
        goal: CountedAction,
        size: Int,
    ) = counter.reached(playerId, goal, size)

    @JvmStatic
    fun touchedFurni(
        player: Player?,
        action: FurniAction,
        furniName: String,
    ) = counter.furni(player, action, furniName)

    @JvmStatic
    fun tookHandItem(
        player: Player?,
        item: Int,
    ) = counter.handItem(player, item)

    @JvmStatic
    fun chosePet(
        player: Player?,
        petType: Int,
    ) = counter.pet(player, petType)

    @JvmStatic
    fun finishedQuest(player: Player) = progress(player, CountedAction.COMPLETE_QUEST, 1)

    @JvmStatic
    fun claimedDailyTask(player: Player) = progress(player, CountedAction.CLAIM_DAILY_TASK, 1)

    @JvmStatic
    fun forget(playerId: Int) = counter.forget(playerId)
}
