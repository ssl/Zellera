package zel.room.map

data class Point(
    val x: Int,
    val y: Int,
) {
    fun step(direction: Int): Point {
        val d = Math.floorMod(direction, DX.size)
        return Point(x + DX[d], y + DY[d])
    }

    private companion object {
        val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    }
}
