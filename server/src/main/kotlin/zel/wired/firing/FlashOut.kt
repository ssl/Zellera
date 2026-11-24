package zel.wired.firing

import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.protocol.generated.WiredMovementsOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxCodes
import zel.wired.box.BoxKind
import zel.wired.variable.VariableBlink

object FlashOut {
    const val SELECTOR_REST = "0"

    const val FILTER_REST = "3"

    /**
     * A wired box blinks at most once in each 500 ms window of the room clock; the furni its effects change go out
     * every firing. The windows are the room clock's 500 ms multiples, and a skipped blink leaves the box state alone.
     */
    const val BLINK_WINDOW_MILLIS = 500L

    /**
     * The firing's furni states, then its movements. A furni the viewers do not have ([inView] false) is left out of
     * the ObjectsDataUpdate; its state still changes. [inView] is built only after [FlashBatch.takeFirst], so it
     * reflects the firing's own area hides. A written variable box moves one step per firing, by its first write's
     * kind. Inside each group the items go by [rank], the room's [FlashOrder].
     */
    fun send(
        scope: RoomScope,
        batch: FlashBatch,
        rank: (Int) -> Int = { 0 },
        inView: () -> (RoomFurni) -> Boolean = { { true } },
    ) {
        val furni = scope.room.furni
        placed(scope, batch, inView())
        batch.takeFirst().forEach { it(scope) }
        val shown = inView()
        batch.boxIds().mapNotNull { furni[it] }.forEach {
            it.state = if (batch.inFilterMode(it.id)) filterBlinked(it.state) else blinked(it)
        }
        batch.writes().forEach { (id, writes) ->
            furni[id]?.let { box -> box.state = VariableBlink.written(box.state, VariableBlink.net(writes)) }
        }
        val items =
            batch.ids(rank).mapNotNull { id ->
                furni[id]?.takeIf { !it.isWall && shown(it) }?.let {
                    ObjectsDataUpdateOut.List1Item(
                        id,
                        FurniWire.stuff(it),
                    )
                }
            }
        if (items.isNotEmpty()) {
            scope.out.send(
                Lane.Now,
                Audience.Room,
                ObjectsDataUpdateOutCodec,
                ObjectsDataUpdateOut(items),
            )
        }
        // A floor furni out of view gets no movement. A furni the firing's own area hide took out has its slide
        // dropped the same way.
        val movements =
            batch.movements.entries().filterNot { move ->
                move is Movement.FurniMove && furni[move.furniId]?.let { !it.isWall && !shown(it) } == true
            }
        if (movements.isNotEmpty()) {
            val message = MovementWire.message(movements)
            scope.out.send(Lane.Now, Audience.Room, WiredMovementsOutCodec, message)
        }
        batch.afterFlash().forEach { it(scope) }
    }

    /**
     * The furni the firing's moves and lifts put somewhere without a slide, one ObjectUpdate each with its final
     * place, and the tiles' final heights. They go ahead of the firing's area hide changes and its
     * ObjectsDataUpdate; a furni a later move took out of view gets none.
     */
    private fun placed(
        scope: RoomScope,
        batch: FlashBatch,
        shown: (RoomFurni) -> Boolean,
    ) {
        batch.takeObjectUpdates().mapNotNull { scope.room.furni[it] }.filter(shown).forEach {
            scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(it))
        }
        batch.takeHeights()?.updates()?.forEach {
            scope.out.send(Lane.NextTickAfterUnitUpdate, Audience.Room, HeightMapUpdateOutCodec, it)
        }
    }

    /** A filter-mode selector's next blink state: "3" only until the first blink, then "4", "5", "4", ... */
    private fun filterBlinked(state: String): String = if (state == "4") "5" else "4"

    fun blinked(box: RoomFurni): String {
        val selector = BoxCodes.kindOf(Interactions.of(box.definition)) == BoxKind.Selector
        return when {
            selector -> if (box.state == "1") "2" else "1"
            box.state == "1" -> "0"
            else -> "1"
        }
    }
}
