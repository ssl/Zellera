package zel.game.games.snowball.sim

/** A Game2GameStatus event, the simulation's only input; applied unchecked, so requests are validated first. */
sealed class GameEvent(
    val type: Int,
    vararg payload: Int,
) {
    private val values = payload.clone()

    fun payload(): IntArray = values.clone()

    internal abstract fun apply(stage: SyncStage)

    override fun toString(): String = type.toString() + values.contentToString()

    class HumanLeftGame internal constructor(
        humanId: Int,
        private val human: Human?,
    ) : GameEvent(HUMAN_LEFT_GAME, humanId) {
        constructor(human: Human) : this(human.id, human)

        override fun apply(stage: SyncStage) {
            human?.let {
                stage.markForDelete(it)
                it.onRemove()
            }
        }
    }

    class NewMoveTarget internal constructor(
        humanId: Int,
        private val human: Human?,
        private val x: Int,
        private val y: Int,
    ) : GameEvent(NEW_MOVE_TARGET, humanId, x, y) {
        constructor(human: Human, x: Int, y: Int) : this(human.id, human, x, y)

        override fun apply(stage: SyncStage) {
            human?.changeMoveTarget(x, y)
        }
    }

    class HumanThrowsSnowballAtHuman internal constructor(
        humanId: Int,
        private val human: Human?,
        targetId: Int,
        private val target: Human?,
        trajectory: Int,
    ) : GameEvent(HUMAN_THROWS_SNOWBALL_AT_HUMAN, humanId, targetId, trajectory) {
        constructor(human: Human, target: Human, trajectory: Int) : this(human.id, human, target.id, target, trajectory)

        override fun apply(stage: SyncStage) {
            if (human == null || target == null) {
                return
            }
            if (human.throwSnowball(target.x, target.y)) {
                stage.onStat(StatEvent.Thrown(human))
            }
            human.startThrowTimer()
        }
    }

    class HumanThrowsSnowballAtPosition internal constructor(
        humanId: Int,
        private val human: Human?,
        private val x: Int,
        private val y: Int,
        trajectory: Int,
    ) : GameEvent(HUMAN_THROWS_SNOWBALL_AT_POSITION, humanId, x, y, trajectory) {
        constructor(human: Human, x: Int, y: Int, trajectory: Int) : this(human.id, human, x, y, trajectory)

        override fun apply(stage: SyncStage) {
            if (human == null) {
                return
            }
            if (human.throwSnowball(x, y)) {
                stage.onStat(StatEvent.Thrown(human))
            }
            human.startThrowTimer()
        }
    }

    class HumanStartsToMakeASnowball internal constructor(
        humanId: Int,
        private val human: Human?,
    ) : GameEvent(HUMAN_STARTS_TO_MAKE_A_SNOWBALL, humanId) {
        constructor(human: Human) : this(human.id, human)

        override fun apply(stage: SyncStage) {
            human?.startMakingSnowball()
        }
    }

    /** The ball starts at the thrower at z 3000 toward (targetX, targetY) and exists once the event is built. */
    class CreateSnowball internal constructor(
        ballId: Int,
        throwerId: Int,
        private val thrower: Human?,
        private val targetX: Int,
        private val targetY: Int,
        private val trajectory: Int,
    ) : GameEvent(CREATE_SNOWBALL, ballId, throwerId, targetX, targetY, trajectory) {
        private val ball = Snowball(ballId)

        constructor(ballId: Int, thrower: Human, targetX: Int, targetY: Int, trajectory: Int) :
            this(ballId, thrower.id, thrower, targetX, targetY, trajectory)

        override fun apply(stage: SyncStage) {
            if (thrower == null) {
                return
            }
            stage.addObject(ball)
            ball.initialize(thrower.x, thrower.y, Snowball.START_Z, trajectory, targetX, targetY, thrower)
        }
    }

    class MachineCreatesSnowball internal constructor(
        machineId: Int,
        private val machine: SnowballMachine?,
    ) : GameEvent(MACHINE_CREATES_SNOWBALL, machineId) {
        constructor(machine: SnowballMachine) : this(machine.id, machine)

        override fun apply(stage: SyncStage) {
            machine?.createSnowball()
        }
    }

    class HumanGetsSnowballsFromMachine internal constructor(
        humanId: Int,
        private val human: Human?,
        giverId: Int,
        private val giver: SnowballGiver?,
    ) : GameEvent(HUMAN_GETS_SNOWBALLS_FROM_MACHINE, humanId, giverId) {
        constructor(human: Human, giver: SnowballGiver) : this(human.id, human, giver.id, giver)

        override fun apply(stage: SyncStage) {
            if (human == null || giver == null || human.snowballCapacity() <= 0) {
                return
            }
            val taken = giver.takeSnowballs(1)
            if (taken > 0) {
                human.addSnowballs(taken)
                stage.onStat(StatEvent.PickedUp(human, giver))
            }
        }
    }

    companion object {
        const val HUMAN_LEFT_GAME = 1
        const val NEW_MOVE_TARGET = 2
        const val HUMAN_THROWS_SNOWBALL_AT_HUMAN = 3
        const val HUMAN_THROWS_SNOWBALL_AT_POSITION = 4
        const val HUMAN_STARTS_TO_MAKE_A_SNOWBALL = 7
        const val CREATE_SNOWBALL = 8
        const val MACHINE_CREATES_SNOWBALL = 11
        const val HUMAN_GETS_SNOWBALLS_FROM_MACHINE = 12

        private val KNOWN_TYPES =
            setOf(
                HUMAN_LEFT_GAME,
                NEW_MOVE_TARGET,
                HUMAN_THROWS_SNOWBALL_AT_HUMAN,
                HUMAN_THROWS_SNOWBALL_AT_POSITION,
                HUMAN_STARTS_TO_MAKE_A_SNOWBALL,
                CREATE_SNOWBALL,
                MACHINE_CREATES_SNOWBALL,
                HUMAN_GETS_SNOWBALLS_FROM_MACHINE,
            )

        fun isKnownType(type: Int): Boolean = type in KNOWN_TYPES

        /** Resolves ids now; an id naming nothing leaves the event without effect, an unknown type gives null. */
        @Suppress("MagicNumber") // payload positions of the wire layout
        fun resolve(
            stage: SyncStage,
            type: Int,
            ints: IntArray,
        ): GameEvent? =
            when (type) {
                HUMAN_LEFT_GAME -> {
                    HumanLeftGame(ints[0], human(stage, ints[0]))
                }

                NEW_MOVE_TARGET -> {
                    NewMoveTarget(ints[0], human(stage, ints[0]), ints[1], ints[2])
                }

                HUMAN_THROWS_SNOWBALL_AT_HUMAN -> {
                    HumanThrowsSnowballAtHuman(ints[0], human(stage, ints[0]), ints[1], human(stage, ints[1]), ints[2])
                }

                HUMAN_THROWS_SNOWBALL_AT_POSITION -> {
                    HumanThrowsSnowballAtPosition(ints[0], human(stage, ints[0]), ints[1], ints[2], ints[3])
                }

                HUMAN_STARTS_TO_MAKE_A_SNOWBALL -> {
                    HumanStartsToMakeASnowball(ints[0], human(stage, ints[0]))
                }

                CREATE_SNOWBALL -> {
                    CreateSnowball(ints[0], ints[1], human(stage, ints[1]), ints[2], ints[3], ints[4])
                }

                MACHINE_CREATES_SNOWBALL -> {
                    MachineCreatesSnowball(ints[0], stage.objectOrNull(ints[0]) as? SnowballMachine)
                }

                HUMAN_GETS_SNOWBALLS_FROM_MACHINE -> {
                    HumanGetsSnowballsFromMachine(
                        ints[0],
                        human(stage, ints[0]),
                        ints[1],
                        stage.objectOrNull(ints[1]) as? SnowballGiver,
                    )
                }

                else -> {
                    null
                }
            }

        private fun human(
            stage: SyncStage,
            id: Int,
        ): Human? = stage.objectOrNull(id) as? Human
    }
}
