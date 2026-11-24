package zel.room.furni.behaviour.state

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.room.RoomScope
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni

class RandomState(
    private val part: StatePart,
) : FurniBehaviour {
    fun draw(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        val params =
            runCatching { RandomDrawParams.parse(furni.definition.params) }.getOrElse {
                log.warn("furni {}: no random state params in '{}'", furni.id, furni.definition.params)
                return
            }
        StateShow.data(scope, furni, "")
        val state = part.random.nextInt(maxOf(1, params.states)) + 1
        part.later(
            scope,
            furni,
            params.delay.toLong(),
        ) { inner, target -> StateShow.data(inner, target, state.toString()) }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RandomState::class.java)
    }
}
