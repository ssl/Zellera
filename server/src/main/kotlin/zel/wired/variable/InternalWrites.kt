package zel.wired.variable

import zel.game.items.RecolorablePalettes
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.StackHelperState
import zel.room.furni.behaviour.special.BackgroundToner
import zel.room.furni.behaviour.state.AreaHide
import zel.room.furni.behaviour.state.Controller
import zel.room.furni.behaviour.state.InvisibleFurni
import zel.room.furni.behaviour.state.Recolorable
import zel.room.furni.behaviour.state.StatePart
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.wired.Hundredths
import zel.wired.box.pattern.ToggleState
import zel.wired.effect.furni.FurniMoves
import zel.wired.effect.furni.WallMoves
import zel.wired.effect.furni.WallPlace
import zel.wired.firing.Firing
import zel.wired.firing.Movement

class InternalWrites(
    private val facts: VariableFacts,
    private val space: (RoomScope) -> VariableSpace,
    private val moves: FurniMoves,
    private val walls: WallMoves,
    private val state: StatePart,
    private val slides: UnitSlides,
) {
    fun write(
        on: VariableOn,
        id: String,
        holder: Holder,
        value: Long,
    ): Boolean {
        val firing = on.firing ?: return false
        return when (holder) {
            is Holder.Furni -> {
                on.scope.room.furni[holder.id]
                    ?.let { writeFurni(firing, it, id, value) } == true
            }

            Holder.Room -> {
                writeRoom(on, id, value)
            }

            Holder.Context -> {
                false
            }

            else -> {
                holder.unitIn(on.scope.room)?.let { writeUnit(firing, it, id, value) } == true
            }
        }
    }

    fun create(
        on: VariableOn,
        id: String,
        holder: Holder,
        value: Long,
        override: Boolean,
    ): Boolean =
        when (id) {
            InternalIds.CAN_MOVE_FREELY -> (holder is Holder.Furni) && setFlag(on, id, holder, on2 = true)
            "-240" -> holder.unitIn(on.scope.room)?.let { giveHand(on, it, value, override) } == true
            else -> switchFurniFlag(on, id, holder, true)
        }

    fun delete(
        on: VariableOn,
        id: String,
        holder: Holder,
    ): Boolean =
        when (id) {
            InternalIds.CAN_MOVE_FREELY -> setFlag(on, id, holder, on2 = false)
            "-240" -> holder.unitIn(on.scope.room)?.let { dropHand(on, it) } == true
            ICON -> false
            else -> switchFurniFlag(on, id, holder, false)
        }

    private fun setFlag(
        on: VariableOn,
        id: String,
        holder: Holder,
        on2: Boolean,
    ): Boolean {
        val held = space(on.scope).internalFlags.getOrPut(id) { HashSet() }
        return if (on2) held.add(holder) else held.remove(holder)
    }

    private fun switchFurniFlag(
        on: VariableOn,
        id: String,
        holder: Holder,
        to: Boolean,
    ): Boolean {
        val furni = (holder as? Holder.Furni)?.let { on.scope.room.furni[it.id] } ?: return false
        val firing = on.firing
        val code = Interactions.of(furni.definition)
        return when {
            id == InternalIds.MULTI_WALK && code == WALK_TILE -> {
                furni.state = StackHelperState.stored(furni.z, to)
                furni.dirty = true
                firing?.flash?.changed(furni.id)
                true
            }

            id in AREA_FLAGS && code == AREA_HIDE -> {
                areaHide(on.scope, furni, firing, AREA_FLAGS.getValue(id), if (to) "1" else "0")
            }

            else -> {
                false
            }
        }
    }

    /**
     * Give `@handitem`: held from then on, also with the value 0 and an empty hand. Only a different item reaches
     * the hand and the room, and a 0 never empties it; without override a holder keeps what it has (a give of 0 on
     * room entry sends no CarryObject, the change after it sends the item).
     */
    private fun giveHand(
        on: VariableOn,
        unit: RoomUnit,
        value: Long,
        override: Boolean,
    ): Boolean {
        if (!override && facts.handItemHeld(unit)) return false
        if (value != 0L && value.toInt() != facts.handItem(unit)) hold(on, unit, value)
        facts.markHandItem(on.scope, unit, held = true)
        return true
    }

    private fun dropHand(
        on: VariableOn,
        unit: RoomUnit,
    ): Boolean {
        facts.markHandItem(on.scope, unit, held = false)
        return hold(on, unit, 0)
    }

    private fun hold(
        on: VariableOn,
        unit: RoomUnit,
        item: Long,
    ): Boolean {
        facts.holdItem(on.scope, unit, item.toInt())
        return true
    }

    private fun writeRoom(
        on: VariableOn,
        id: String,
        value: Long,
    ): Boolean {
        val color = TEAM_SCORES[id] ?: return false
        return facts.setTeamScore(on.scope, color, value.toInt())
    }

    private fun writeUnit(
        firing: Firing,
        unit: RoomUnit,
        id: String,
        value: Long,
    ): Boolean =
        when (id) {
            "-230" -> slides.slide(firing, unit, Point(value.toInt(), unit.tile.y))
            "-231" -> slides.slide(firing, unit, Point(unit.tile.x, value.toInt()))
            "-234" -> slides.slide(firing, unit, Point((value shr BYTE).toInt(), (value and BYTE_MASK).toInt()))
            "-232" -> slides.turn(firing, unit, value.toInt())
            "-240" -> hold(VariableOn(firing.scope, firing), unit, value)
            "-237" -> facts.setUserScore(firing.scope, unit, value.toInt())
            else -> false
        }

    private fun writeFurni(
        firing: Firing,
        furni: RoomFurni,
        id: String,
        value: Long,
    ): Boolean =
        if (furni.isWall) {
            writeWall(firing, furni, id, value)
        } else {
            writeFloor(firing, furni, id, value) || writeKind(firing, furni, id, value)
        }

    private fun writeFloor(
        firing: Firing,
        furni: RoomFurni,
        id: String,
        value: Long,
    ): Boolean {
        val tile = furni.anchor ?: return false
        return when (id) {
            "-120" -> moves.move(firing, furni, Point(value.toInt(), tile.y))
            "-121" -> moves.move(firing, furni, Point(tile.x, value.toInt()))
            "-125" -> moves.move(firing, furni, Point((value shr BYTE).toInt(), (value and BYTE_MASK).toInt()))
            "-122" -> rotate(firing, furni, tile, value)
            "-123" -> lift(firing, furni, value)
            "-126" -> occupy(firing, furni, value)
            "-110" -> writeState(firing, furni, value)
            else -> false
        }
    }

    private fun rotate(
        firing: Firing,
        furni: RoomFurni,
        tile: Point,
        value: Long,
    ): Boolean {
        val direction = direction(furni, value) ?: return false
        return moves.move(firing, furni, tile, direction)
    }

    private fun direction(
        furni: RoomFurni,
        rotation: Long,
    ): Int? {
        val direction = if (furni.definition.hasDiagonalDirections()) rotation else rotation * 2
        return direction.takeIf { it in 0 until UnitDirection.COUNT }?.toInt()
    }

    private fun lift(
        firing: Firing,
        furni: RoomFurni,
        value: Long,
    ): Boolean {
        val height = Hundredths.of(furni.top - furni.z)
        val target = value.coerceIn(0, maxOf(0L, MAX_ALTITUDE - height))
        if (Hundredths.of(furni.z) == target) return false
        moves.lift(firing, furni, Hundredths.toTiles(target))
        return true
    }

    private fun occupy(
        firing: Firing,
        furni: RoomFurni,
        value: Long,
    ): Boolean {
        val x = (value shr 2 * BYTE).toInt()
        val y = ((value shr BYTE) and BYTE_MASK).toInt()
        val direction = direction(furni, value and BYTE_MASK) ?: return false
        return moves.move(firing, furni, Point(x, y), direction)
    }

    private fun writeState(
        firing: Firing,
        furni: RoomFurni,
        value: Long,
    ): Boolean {
        if (facts.clock(furni) != null || !ToggleState.toggles(furni)) return false
        if (value !in 0 until furni.definition.stateCount) return false
        val area = furni.behaviour as? AreaHide
        // Only a real change is sent: a write of the state it has sends nothing.
        val changed =
            when {
                area != null -> {
                    area.switchTo(firing.scope, furni, on = value != 0L, flash = firing.flash)
                }

                (furni.state.toLongOrNull() ?: 0) == value -> {
                    false
                }

                else -> {
                    StateShow.set(furni, value.toString())
                    Controller.written(firing.scope, furni)
                    true
                }
            }
        if (changed) firing.flash.changed(furni.id)
        return true
    }

    private fun writeKind(
        firing: Firing,
        furni: RoomFurni,
        id: String,
        value: Long,
    ): Boolean {
        val code = Interactions.of(furni.definition)
        return when {
            code == AREA_HIDE && id in AREA_VALUES -> {
                areaHide(firing.scope, furni, firing, AREA_VALUES.getValue(id), value.toString())
            }

            id == "~121" -> {
                setClock(firing.scope, furni, value)
            }

            code == TONER && id in TONER_PARTS -> {
                setToner(firing, furni, id, value)
            }

            id == RGB -> {
                recolour(firing, furni, value)
            }

            id == InternalIds.TELEPORT_TARGET && (code == "transit.door" || code == "transit.tile") -> {
                aim(firing.scope, furni, code, value)
            }

            else -> {
                false
            }
        }
    }

    private fun setToner(
        firing: Firing,
        furni: RoomFurni,
        id: String,
        value: Long,
    ): Boolean {
        val values = BackgroundToner.values(furni.state).toMutableList()
        values[TONER_PARTS.getValue(id)] = value.coerceIn(0, TONER_MAX).toInt()
        StateShow.whole(firing.scope, furni, BackgroundToner.state(values))
        return true
    }

    /** Only a teleport of the same kind in this room, itself included; anything else is silently refused. */
    private fun aim(
        scope: RoomScope,
        furni: RoomFurni,
        code: String,
        value: Long,
    ): Boolean {
        val target =
            value
                .takeIf { it in 1..Int.MAX_VALUE }
                ?.let { scope.room.furni[it.toInt()] }
                ?.takeIf { Interactions.of(it.definition) == code } ?: return false
        facts.aimTeleport(scope.room.info.id, furni, target)
        return true
    }

    /** Only a colour of the furni's palette; anything else is silently refused. */
    private fun recolour(
        firing: Firing,
        furni: RoomFurni,
        value: Long,
    ): Boolean {
        val block = furni.behaviour as? Recolorable ?: return false
        val rgb = value.toInt()
        if (!RecolorablePalettes.get().allows(furni.definition.name, rgb)) return false
        StateShow.set(furni, block.stored(block.state(furni), rgb))
        firing.flash.changed(furni.id)
        return true
    }

    private fun setClock(
        scope: RoomScope,
        furni: RoomFurni,
        halfSeconds: Long,
    ): Boolean {
        val clock = facts.clock(furni) ?: return false
        clock.setMillis((halfSeconds * HALF_SECOND).toInt())
        furni.dirty = true
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectDataUpdateOutCodec,
            ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
        )
        return true
    }

    private fun areaHide(
        scope: RoomScope,
        furni: RoomFurni,
        firing: Firing?,
        key: String,
        value: String,
    ): Boolean {
        val area = furni.behaviour as? AreaHide ?: return false
        val wasInvisible = InvisibleFurni.of(furni)
        if (!area.write(scope, furni, key, value, flash = firing?.flash)) return true
        InvisibleFurni.changed(state, scope, furni, wasInvisible)
        firing?.flash?.changed(furni.id)
        return true
    }

    private fun writeWall(
        firing: Firing,
        item: RoomFurni,
        id: String,
        value: Long,
    ): Boolean {
        val place = WallPlace.of(item) ?: return false
        return when (id) {
            "-120" -> moveWall(firing, item, Point(value.toInt(), place.tile.y))
            "-121" -> moveWall(firing, item, Point(place.tile.x, value.toInt()))
            "-125" -> moveWall(firing, item, Point((value shr BYTE).toInt(), (value and BYTE_MASK).toInt()))
            "-123" -> liftWall(firing, item, value)
            "-190" -> offset(firing, item, place, value)
            else -> false
        }
    }

    private fun moveWall(
        firing: Firing,
        item: RoomFurni,
        tile: Point,
    ): Boolean {
        walls.moveTo(firing, item, tile)
        return true
    }

    private fun liftWall(
        firing: Firing,
        item: RoomFurni,
        value: Long,
    ): Boolean {
        if (value !in 0..MAX_ALTITUDE) return false
        walls.lift(firing, item, value.toInt())
        return true
    }

    private fun offset(
        firing: Firing,
        item: RoomFurni,
        place: WallPlace,
        value: Long,
    ): Boolean {
        val room = firing.scope.room
        val width = if (room.floor.smallScale) WallPlace.SMALL_WIDTH else WallPlace.WIDTH
        if (value !in 0..width) return false
        val altitude = place.altitude(room)
        val moved = place.copy(localX = value.toInt(), altitude = altitude)
        val next = moved.copy(localY = moved.localYAt(room, altitude))
        item.wallPosition = next.text()
        item.dirty = true
        firing.moved += item.id
        firing.flash.movements.add(
            Movement.WallMove(
                item.id,
                next.right,
                Movement.WallSpot(place.tile.x, place.tile.y, place.localX, place.localY),
                Movement.WallSpot(next.tile.x, next.tile.y, next.localX, next.localY),
                moves.style(firing).duration,
            ),
        )
        return true
    }

    private companion object {
        const val ICON = "-245"
        const val BYTE = 8
        const val BYTE_MASK = 0xFFL
        const val MAX_ALTITUDE = 8000L
        const val HALF_SECOND = 500L
        const val AREA_HIDE = "area.hide"
        const val TONER = "room.toner"
        const val TONER_MAX = 255L
        const val RGB = "~146"
        val TONER_PARTS = mapOf("~100" to 1, "~101" to 2, "~102" to 3)
        val TEAM_SCORES = mapOf("-320" to 1, "-321" to 2, "-322" to 3, "-323" to 4)
        const val WALK_TILE = "tile.walk"
        val AREA_VALUES =
            mapOf(
                "~110" to AreaHideValues.ROOT_X,
                "~111" to AreaHideValues.ROOT_Y,
                "~112" to AreaHideValues.WIDTH,
                "~113" to AreaHideValues.LENGTH,
            )
        val AREA_FLAGS =
            mapOf(
                "~114" to AreaHideValues.INVISIBLE,
                "~115" to AreaHideValues.WALL_ITEMS,
                "~116" to AreaHideValues.INVERT,
            )
    }
}
