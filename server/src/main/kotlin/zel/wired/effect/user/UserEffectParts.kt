package zel.wired.effect.user

import zel.room.Room
import zel.room.furni.behaviour.state.StateHooks
import zel.room.furni.place.UnitsOnTiles
import zel.room.games.GamesPart
import zel.room.unit.walk.Walking
import zel.wired.WiredRooms
import zel.wired.store.RoomLog
import zel.wired.timing.RoomPoster
import zel.wired.variable.RoomVariables
import kotlin.random.Random

class UserEffectParts(
    val walking: Walking,
    val hooks: StateHooks,
    val games: GamesPart,
    val poster: RoomPoster,
    val host: UserEffectHost,
    val units: UnitsOnTiles,
    val wired: () -> WiredRooms? = { null },
    val placeholders: PlaceholderValues = PlaceholderValues.None,
    val random: Random = Random.Default,
    val variables: RoomVariables? = null,
    val roomLog: (Room, RoomLog.Line) -> Unit = { _, _ -> },
)
