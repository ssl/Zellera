package zel.room.furni.behaviour.special

import zel.game.items.Item
import zel.protocol.generated.ItemRemoveOutCodec
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.generated.ObjectRemoveOut
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.furni.FurniBehaviour
import zel.room.furni.HeightMapChange
import zel.room.furni.PlainFurni
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateHooks
import zel.room.furni.place.FurniOut
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/** Every member runs under the room's lock unless it says otherwise. */
class SpecialPart(
    val hooks: StateHooks,
    val host: SpecialHost = SpecialHost.None,
    val persist: (Runnable) -> Unit = Runnable::run,
    val post: (Int, (RoomScope) -> Unit) -> Unit = { _, _ -> },
    val behaviourOf: (String) -> FurniBehaviour = { PlainFurni },
    site: () -> String = { "" },
    dimmerStore: DimmerStore = DimmerStore.None,
    val pets: SpecialPets = SpecialPets.None,
    /** The room's ordered lane: a spent row is deleted behind the write that placed it. */
    private val writes: RoomWrites = RoomWrites { _, job -> persist(job) },
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, SpecialRoom>()

    val mystery = MysteryBoxes(this)
    val crackables = Crackables(this)
    val dimmers = Dimmers(this, dimmerStore)
    val toner = BackgroundToner(hooks)
    val ads = RoomAds(site)
    val petFurni = PetFurni(this, pets, host::petPackage)

    val behaviours: List<Pair<String, FurniBehaviour>> =
        listOf(
            "display.badge" to BadgeDisplay(),
            "outfit.clothing" to ClothingFurni(),
            "room.toner" to toner,
            "room.backdrop" to this.ads,
            "mystery.box" to mystery.box,
            "mystery.key" to mystery.key,
            "crack.plain" to crackables.plain,
            "crack.master" to crackables.master,
            "crack.monster" to crackables.monster,
            "crack.subscription" to crackables.subscription,
            "room.dimmer" to dimmers.dimmer,
            "pet.seed" to petFurni.seed,
            "pet.box" to petFurni.box,
            PetFurni.PET_BOX_TYPE to petFurni.petBox,
            "pet.nest.breeding" to PlainFurni,
            "totem.legs" to Totem(Totem.Part.Legs, hooks),
            "totem.head" to Totem(Totem.Part.Head, hooks),
            "totem.planet" to Totem(Totem.Part.Planet, hooks) { p, e -> persist(Runnable { host.wearEffect(p, e) }) },
        )

    val tick: TickPart =
        TickPart { scope ->
            val later = of(scope.room).later
            val due = later.filter { scope.now() >= it.due }
            later.removeAll(due)
            due.forEach { job -> scope.room.furni[job.furniId]?.let { job.run(scope, it) } }
            val shown = of(scope.room).due
            val reached = shown.filter { scope.now() >= it.due }
            shown.removeAll(reached)
            reached.forEach { it.run(scope) }
            crackables.tick(scope)
            petFurni.tick(scope)
        }

    fun of(room: Room): SpecialRoom = rooms.getOrPut(room) { SpecialRoom() }

    override fun load(room: Room) {
        rooms[room] = SpecialRoom()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)?.becoming?.forEach { it.roomGone() }
    }

    fun later(
        scope: RoomScope,
        furni: RoomFurni,
        millis: Long,
        run: (RoomScope, RoomFurni) -> Unit,
    ) {
        of(scope.room).later += SpecialRoom.SpecialJob(scope.now() + millis, furni.id, run)
    }

    /** Runs [run] under the room's lock once its clock reaches [dueMillis]. */
    fun at(
        scope: RoomScope,
        dueMillis: Long,
        run: (RoomScope) -> Unit,
    ) {
        of(scope.room).due += SpecialRoom.RoomJob(dueMillis, run)
    }

    /** [onDeleted] runs once the row is gone, on the room's write lane; false if unspendable. */
    fun spend(
        scope: RoomScope,
        furni: RoomFurni,
        delayMillis: Int = 0,
        picker: Int = 0,
        onDeleted: () -> Unit = {},
    ): Boolean {
        if (!furni.isSpendable) return false
        val room = scope.room
        val tiles = furni.tiles()
        val heights = HeightMapChange.of(room, tiles)
        room.furni.remove(furni.id) ?: return false
        if (furni.isWall) {
            scope.out.send(Lane.Now, Audience.Room, ItemRemoveOutCodec, FurniWire.itemRemove(furni))
        } else {
            val removal = ObjectRemoveOut(furni.id.toString(), false, picker, delayMillis, false)
            scope.out.send(Lane.Now, Audience.Room, ObjectRemoveOutCodec, removal)
            FurniOut.heights(scope, heights)
            hooks.heightsChanged(scope, tiles)
        }
        val roomId = room.info.id
        writes.write(
            roomId,
            Runnable {
                if (host.deleteSpent(roomId, furni.rowId)) {
                    onDeleted()
                } else {
                    log.warn("room {}: spent furni {} had left", roomId, furni.id)
                }
            },
        )
        return true
    }

    fun <T : Any> becomes(
        scope: RoomScope,
        furni: RoomFurni,
        make: () -> T?,
        orphaned: (T) -> Unit,
        show: (RoomScope, Left, T?) -> Unit,
    ): Boolean {
        if (!furni.isSpendable) return false
        val room = scope.room
        val tiles = furni.tiles()
        val heights = HeightMapChange.of(room, tiles)
        room.furni.remove(furni.id) ?: return false
        val left = Left(furni, tiles, heights)
        val roomId = room.info.id
        val becoming = SpecialRoom.Becoming(orphaned)
        of(room).becoming += becoming
        writes.write(
            roomId,
            Runnable {
                val deleted = host.deleteSpent(roomId, furni.rowId)
                if (!deleted) log.warn("room {}: spent furni {} had left", roomId, furni.id)
                becoming.made(if (deleted) make() else null)
                post(roomId) { later ->
                    if (later.room === room) of(room).becoming -= becoming
                    becoming.showOnce { made -> show(later, left, made) }
                }
            },
        )
        return true
    }

    inner class Left(
        private val furni: RoomFurni,
        private val tiles: List<Point>,
        private val heightChange: HeightMapChange,
    ) {
        fun removed(
            scope: RoomScope,
            picker: Int = 0,
        ) {
            val removal = ObjectRemoveOut(furni.id.toString(), false, picker, 0, false)
            scope.out.send(Lane.Now, Audience.Room, ObjectRemoveOutCodec, removal)
        }

        fun heights(scope: RoomScope) {
            FurniOut.heights(scope, heightChange)
            hooks.heightsChanged(scope, tiles)
        }
    }

    data class Spot(
        val anchor: Point,
        val z: Double,
        val direction: Int,
    )

    data class NewFurni(
        val id: Int,
        val definition: Item,
    )

    fun arrive(
        scope: RoomScope,
        ownerId: Int,
        ownerName: String,
        made: NewFurni,
        spot: Spot,
        state: String,
    ) {
        if (scope.room.furni[made.id] != null) return
        val furni =
            RoomFurni(
                made.id,
                ownerId,
                ownerName,
                made.definition,
                behaviourOf(Interactions.of(made.definition)),
                spot.anchor,
                spot.z,
                spot.direction,
                state,
                null,
                0,
                "0:0",
            )
        val heights = HeightMapChange.of(scope.room, furni.tiles())
        scope.room.furni.add(furni)
        scope.out.send(Lane.Now, Audience.Room, ObjectAddOutCodec, FurniWire.objectAdd(furni))
        FurniOut.heights(scope, heights)
        hooks.heightsChanged(scope, furni.tiles())
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SpecialPart::class.java)
    }
}
