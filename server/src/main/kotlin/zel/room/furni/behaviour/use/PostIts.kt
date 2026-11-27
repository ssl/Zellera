package zel.room.furni.behaviour.use

import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.items.NoteColour
import zel.game.players.Player
import zel.protocol.generated.ItemDataUpdateOut
import zel.protocol.generated.ItemDataUpdateOutCodec
import zel.protocol.generated.ItemRemoveOutCodec
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone

class PostIts(
    private val hooks: Hooks = Hooks { text, _ -> text },
    private val deleted: (roomId: Int, rowId: Int) -> Unit,
) {
    fun interface Hooks {
        fun filter(
            text: String,
            player: Player,
        ): String
    }

    fun read(
        scope: RoomScope,
        playerId: Int,
        itemId: Int,
    ) {
        val note = note(scope, itemId) ?: return
        val data = ItemDataUpdateOut(note.id.toString(), note.state)
        scope.out.send(Lane.Now, Audience.Player(playerId), ItemDataUpdateOutCodec, data)
    }

    fun write(
        scope: RoomScope,
        player: Player,
        level: Int,
        itemId: Int,
        colour: String,
        text: String,
    ) {
        val note = note(scope, itemId) ?: return
        if (level < DoorCheck.RIGHTS || colour !in COLOURS || text.length > MAX_TEXT) return
        StateShow.data(scope, note, "$colour ${hooks.filter(text, player)}")
    }

    fun writeOnPole(
        scope: RoomScope,
        player: Player,
        level: Int,
        itemId: Int,
        colour: String,
        text: String,
    ) {
        val note = note(scope, itemId) ?: return
        val blank = note.state.none { it == ' ' }
        if (level < DoorCheck.RIGHTS && !(blank && note.placerId == player.profile.id)) return
        val config = player.services.config
        val shown = hooks.filter(text, player)
        if (shown.length > config.getInt(CHAR_LIMIT_KEY, MAX_TEXT)) return
        val written = NoteColour.onPole(colour)
        val header =
            player.services.texts
                .text(POLE_HEADER_TEXT, DEFAULT_POLE_HEADER)
                .replace("%player%", player.profile.name)
                .replace("%time%", GameZone.today().toString())
        StateShow.data(scope, note, "$written $header\r$shown")
    }

    fun engrave(
        scope: RoomScope,
        player: Player,
        itemId: Int,
        engraved: (Item) -> String,
    ) {
        val trophy =
            scope.room.furni[itemId]
                ?.takeIf { Interactions.of(it.definition) == TROPHY }
                ?: return
        if (trophy.ownerId != player.profile.id || trophy.state.isNotEmpty()) return
        StateShow.data(scope, trophy, engraved(trophy.definition))
    }

    fun remove(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        itemId: Int,
    ) {
        val note =
            scope.room.furni[itemId]
                ?.takeIf { Interactions.of(it.definition) in THROWN_AWAY }
                ?: return
        val roomOwnerMay = level >= DoorCheck.OWNER && Interactions.of(note.definition) == Interactions.POST_IT
        if (note.ownerId != playerId && !roomOwnerMay) return
        scope.room.furni.remove(note.id) ?: return
        scope.out.send(Lane.Now, Audience.Room, ItemRemoveOutCodec, FurniWire.itemRemove(note, note.ownerId))
        deleted(scope.room.info.id, note.rowId)
    }

    private fun note(
        scope: RoomScope,
        itemId: Int,
    ): RoomFurni? = scope.room.furni[itemId]?.takeIf { Interactions.of(it.definition) == Interactions.POST_IT }

    private companion object {
        const val MAX_TEXT = 366
        const val CHAR_LIMIT_KEY = "furni.sticky_note.max.characters"
        const val POLE_HEADER_TEXT = "room.furni.sticky_pole.note_header"
        const val DEFAULT_POLE_HEADER = "%time%, %player%:"
        const val TROPHY = "display.trophy"
        val THROWN_AWAY = setOf(Interactions.POST_IT, ItemInteraction.EXTERNAL_IMAGE)
        val COLOURS = setOf("FFFF33", "FF9CFF", "9CCEFF", "9CFF9C")
    }
}
