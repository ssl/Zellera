package zel.room.games

/** The clock furni families; only [Timer] and [GameCounter] run the room's games. */
enum class ClockKind(
    val runsGames: Boolean,
    val countsUp: Boolean,
) {
    Timer(runsGames = true, countsUp = false),
    GameCounter(runsGames = true, countsUp = true),
    PlainCounter(runsGames = false, countsUp = true),
    Hourglass(runsGames = false, countsUp = true),
}
