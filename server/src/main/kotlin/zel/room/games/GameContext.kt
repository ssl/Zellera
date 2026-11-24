package zel.room.games

import zel.room.furni.behaviour.state.StateHooks
import zel.room.unit.walk.Walking
import kotlin.random.Random

class GameContext(
    val hooks: StateHooks,
    val walking: Walking,
    val host: GameHost,
    val jobs: RoomJobs,
    val rules: FieldGameRules,
    val random: Random,
)
