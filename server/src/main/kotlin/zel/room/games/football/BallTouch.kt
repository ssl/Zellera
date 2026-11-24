package zel.room.games.football

enum class BallTouch {
    /** A walk's first step lands on the ball. */
    Kick,

    /** A later step of a walk crosses the ball. */
    Push,

    /** A unit steps off the ball's tile in a walk of more than that one step. */
    StepOff,

    Click,
}
