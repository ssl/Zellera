package zel.room.furni

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.items.Item
import zel.game.items.ProductKind
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.room.persist.FurniData
import zel.room.persist.FurniRow
import zel.room.tick.TickPart

class FurniPart(
    private val data: FurniData,
    private val definitions: (Int) -> Item?,
    private val behaviours: FurniBehaviours,
    private val saves: FurniSaves,
    private val clearCopies: (Int) -> Unit = {},
) : RoomPart {
    val tick: TickPart =
        TickPart { scope ->
            scope.room.furni
                .withBehaviour(Ticking::class.java)
                .forEach { furni -> (furni.behaviour as Ticking).onTick(scope, furni) }
        }

    override fun load(room: Room) {
        clearCopies(room.info.id)
        data.roomFurni(room.info.id).forEach { row ->
            val definition = definitions(row.definitionId)
            if (definition == null) {
                log.warn("room {}: furni {} has no definition {}; not loaded", room.info.id, row.id, row.definitionId)
            } else {
                room.furni.addLoaded(furni(row, definition))
            }
        }
        WaterPools.refreshAll(room)
    }

    override fun unload(scope: RoomScope) {
        saves.saveDirty(scope.room)
    }

    private fun furni(
        row: FurniRow,
        definition: Item,
    ): RoomFurni {
        val wall = definition.type == ProductKind.WALL_FURNI
        val clientId = row.clientId
        return RoomFurni(
            id = clientId ?: row.id,
            ownerId = if (clientId == null) row.ownerId else BuildPass.OWNER_ID,
            ownerName = if (clientId == null) row.ownerName else BuildPass.ownerName,
            definition = definition,
            behaviour = behaviours.forType(Interactions.behaviourKey(definition)),
            anchor = if (wall) null else Point(row.x, row.y),
            z = row.z,
            direction = if (wall) row.direction else definition.shownDirection(row.direction),
            state = row.state,
            wallPosition = row.wallPosition.takeIf { wall },
            groupId = row.groupId,
            limitedData = row.limitedData,
            rowId = row.id,
            placerId = row.ownerId,
            placerName = row.ownerName,
            wiredCopy = row.wiredCopy,
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FurniPart::class.java)
    }
}
