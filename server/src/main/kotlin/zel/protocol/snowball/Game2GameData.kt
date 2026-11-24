package zel.protocol.snowball

import zel.game.games.snowball.sim.GameEvent
import zel.game.games.snowball.sim.GameObject
import zel.game.games.snowball.sim.GameObjectData
import zel.protocol.generated.PacketWriter

internal object Game2GameData {
    const val SUBTURN_LISTS = 3

    private const val SNOWBALL_VARS = 11
    private const val TREE_VARS = 9
    private const val PILE_VARS = 7
    private const val MACHINE_VARS = 8
    private const val HUMAN_VARS = 19
    private const val HUMAN_STRINGS = 4

    fun isKnownObjectType(type: Int): Boolean = type in GameObject.TYPE_SNOWBALL..GameObject.TYPE_HUMAN

    fun checkObjects(objects: List<GameObjectData>) {
        for (obj in objects) {
            require(isKnownObjectType(obj.type)) { "The client cannot read game object type ${obj.type}" }
            val expected =
                when (obj.type) {
                    GameObject.TYPE_SNOWBALL -> SNOWBALL_VARS
                    GameObject.TYPE_TREE -> TREE_VARS
                    GameObject.TYPE_PILE -> PILE_VARS
                    GameObject.TYPE_MACHINE -> MACHINE_VARS
                    else -> HUMAN_VARS
                }
            val isHuman = obj.type == GameObject.TYPE_HUMAN
            require(obj.vars.size == expected && isHuman == (obj.strings.size == HUMAN_STRINGS)) {
                "Malformed game object $obj"
            }
        }
    }

    fun checkEvents(subturns: List<List<GameEvent>>) {
        require(subturns.size == SUBTURN_LISTS) { "GameStatusData needs exactly 3 subturn lists, not ${subturns.size}" }
        for (event in subturns.flatten()) {
            require(GameEvent.isKnownType(event.type)) { "The client cannot read game event type ${event.type}" }
        }
    }

    fun writeObjects(
        w: PacketWriter,
        objects: List<GameObjectData>,
    ) {
        w.int(objects.size)
        for (obj in objects) {
            obj.vars.forEach { w.int(it) }
            obj.strings.forEach { w.string(it) }
        }
    }

    fun writeStatus(
        w: PacketWriter,
        turn: Int,
        checksum: Int,
        subturns: List<List<GameEvent>>,
    ) {
        w.int(turn)
        w.int(checksum)
        w.int(subturns.size)
        for (events in subturns) {
            w.int(events.size)
            for (event in events) {
                w.int(event.type)
                event.payload().forEach { w.int(it) }
            }
        }
    }
}
