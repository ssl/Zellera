package zel.room

import zel.room.furni.behaviour.state.StatePart
import zel.room.games.FieldGameRules
import zel.room.games.GameContext
import zel.room.games.GamesPart
import zel.room.games.LiveGameHost
import zel.room.port.RoomPort
import kotlin.random.Random

internal fun gamesPart(
    services: RoomEngineServices,
    core: CoreWiring,
    state: StatePart,
    wired: WiredEngineWiring,
    port: () -> RoomPort?,
): GamesPart =
    GamesPart(
        GameContext(
            hooks = state.hooks,
            walking = core.walking,
            host =
                wired.scores.highscores.games(
                    LiveGameHost(
                        services.clients,
                        services.dataSource,
                        services.persist,
                        appearanceChanged = { port()?.appearanceChanged(it) },
                        clockSeconds = core.nowSeconds,
                        clockCounted = wired::clockCounted,
                    ),
                ),
            jobs = { room, millis, job -> core.rooms.get()?.post(room.info.id, millis, job) },
            rules = FieldGameRules.of(services.config),
            random = Random.Default,
        ),
    ).also(core.gameParts::set)
