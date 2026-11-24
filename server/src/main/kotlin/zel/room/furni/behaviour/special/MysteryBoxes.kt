package zel.room.furni.behaviour.special

import zel.game.items.mysterybox.BoxColour
import zel.protocol.generated.CancelMysteryBoxWaitOut
import zel.protocol.generated.CancelMysteryBoxWaitOutCodec
import zel.protocol.generated.ShowMysteryBoxWaitOut
import zel.protocol.generated.ShowMysteryBoxWaitOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.tick.Audience
import zel.room.tick.Lane
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * A box opens when its owner opens it and another player turns a key of its colour, in either order: the first of
 * the two waits for the other. Both furni are used up and both players draw a prize.
 */
class MysteryBoxes(
    private val part: SpecialPart,
) {
    val box: Half = Half { scope, furni, playerId -> boxClicked(scope, furni, playerId) }
    val key: Half = Half { scope, furni, playerId -> keyClicked(scope, furni, playerId) }

    /** One half of the pair: only its click does something, wired never toggles it. */
    class Half(
        private val click: (RoomScope, RoomFurni, Int) -> Unit,
    ) : Usable,
        Toggleable {
        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) = click(scope, furni, playerId)

        override fun toggle(
            scope: RoomScope,
            furni: RoomFurni,
            backward: Boolean,
        ): Boolean = false

        override fun toggleRandom(
            scope: RoomScope,
            furni: RoomFurni,
            random: Random,
        ): Boolean = false
    }

    private fun boxClicked(
        scope: RoomScope,
        box: RoomFurni,
        playerId: Int,
    ) {
        if (!box.isSpendable) return
        if (playerId == box.ownerId) {
            ownerOpens(scope, box)
        } else {
            keyOf(scope, playerId, box)?.let { turn(scope, box, it) }
        }
    }

    /** A click on the own key turns it in the first box of its colour that someone else owns. */
    private fun keyClicked(
        scope: RoomScope,
        key: RoomFurni,
        playerId: Int,
    ) {
        if (!key.isSpendable || key.ownerId != playerId) return
        val colour = colourOf(key)
        val boxes =
            scope.room.furni
                .all()
                .filter { fits(it, colour) && it.ownerId != playerId }
        val box = boxes.firstOrNull { waitOn(scope, it)?.keyId == 0 } ?: boxes.firstOrNull() ?: return
        turn(scope, box, key)
    }

    private fun ownerOpens(
        scope: RoomScope,
        box: RoomFurni,
    ) {
        val wait = waitOn(scope, box)
        val key = wait?.keyId?.let { scope.room.furni[it] }
        when {
            wait == null -> wait(scope, SpecialRoom.MysteryWait(box.id, box.ownerId, keyId = 0))
            key != null -> open(scope, box, key, wait)
        }
    }

    private fun turn(
        scope: RoomScope,
        box: RoomFurni,
        key: RoomFurni,
    ) {
        val wait = waitOn(scope, box)
        when {
            wait == null -> wait(scope, SpecialRoom.MysteryWait(box.id, key.ownerId, key.id))
            wait.keyId == 0 -> open(scope, box, key, wait)
        }
    }

    private fun wait(
        scope: RoomScope,
        wait: SpecialRoom.MysteryWait,
    ) {
        val waits = part.of(scope.room).mysteryWaits
        waits.values.removeIf { it.playerId == wait.playerId }
        waits[wait.boxId] = wait
        scope.out.send(Lane.Now, Audience.Player(wait.playerId), ShowMysteryBoxWaitOutCodec, ShowMysteryBoxWaitOut)
    }

    private fun open(
        scope: RoomScope,
        box: RoomFurni,
        key: RoomFurni,
        wait: SpecialRoom.MysteryWait,
    ) {
        part.of(scope.room).mysteryWaits.remove(box.id)
        close(scope, wait.playerId)
        val colour = colourOf(box)
        val winners = listOf(box.ownerId, key.ownerId)
        val rowsLeft = AtomicInteger(2)
        val whenBothGone = {
            if (rowsLeft.decrementAndGet() == 0) winners.forEach { part.host.mysteryPrize(it, colour) }
        }
        part.spend(scope, box, onDeleted = whenBothGone)
        part.spend(scope, key, onDeleted = whenBothGone)
    }

    fun cancel(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (part
                .of(scope.room)
                .mysteryWaits.values
                .removeIf { it.playerId == playerId }
        ) {
            close(scope, playerId)
        }
    }

    /** The wait on [box] while the one waiting, the box and a turned key are all still in the room. */
    private fun waitOn(
        scope: RoomScope,
        box: RoomFurni,
    ): SpecialRoom.MysteryWait? {
        val waits = part.of(scope.room).mysteryWaits
        val wait = waits[box.id] ?: return null
        val keyStands = wait.keyId == 0 || scope.room.furni[wait.keyId]?.let { fits(it, colourOf(box), key) } == true
        if (keyStands && scope.room.units.ofPlayer(wait.playerId) != null) return wait
        waits.remove(box.id)
        close(scope, wait.playerId)
        return null
    }

    /** The key of [playerId] in this room that fits [box]. */
    private fun keyOf(
        scope: RoomScope,
        playerId: Int,
        box: RoomFurni,
    ): RoomFurni? {
        val colour = colourOf(box)
        return scope.room.furni
            .all()
            .firstOrNull { it.ownerId == playerId && fits(it, colour, key) }
    }

    private fun fits(
        furni: RoomFurni,
        colour: String,
        half: Half = box,
    ): Boolean = furni.behaviour === half && furni.isSpendable && colourOf(furni) == colour

    private fun close(
        scope: RoomScope,
        playerId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        CancelMysteryBoxWaitOutCodec,
        CancelMysteryBoxWaitOut,
    )

    companion object {
        /** The last word of the furni's name when it is a colour the client draws; else "" (those pair too). */
        fun colourOf(furni: RoomFurni): String {
            val lastWord = furni.definition.name.substringAfterLast('_')
            return BoxColour.named(lastWord)?.code.orEmpty()
        }
    }
}
