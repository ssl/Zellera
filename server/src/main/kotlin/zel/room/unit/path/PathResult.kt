package zel.room.unit.path

import zel.room.map.Point

sealed interface PathResult {
    data class Found(
        val path: List<Point>,
    ) : PathResult

    data object GoalRefused : PathResult

    data object NoPath : PathResult
}
