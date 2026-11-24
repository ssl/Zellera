package zel.room.furni.behaviour.special

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateShow
import zel.room.tick.Audience
import zel.room.tick.Lane
import kotlin.random.Random

class Dimmers(
    private val part: SpecialPart,
    private val store: DimmerStore = DimmerStore.None,
) {
    val dimmer = Dimmer()

    inner class Dimmer :
        Usable,
        Toggleable {
        override fun onUse(
            scope: RoomScope,
            furni: RoomFurni,
            playerId: Int,
            param: Int,
        ) = Unit

        override fun toggle(
            scope: RoomScope,
            furni: RoomFurni,
            backward: Boolean,
        ): Boolean {
            StateShow.set(furni, switched(furni.state, scope))
            return true
        }

        override fun toggleRandom(
            scope: RoomScope,
            furni: RoomFurni,
            random: Random,
        ): Boolean = toggle(scope, furni, backward = false)
    }

    data class Preset(
        val id: Int,
        val backgroundOnly: Boolean,
        val colour: String,
        val brightness: Int,
    ) {
        val type: Int get() = if (backgroundOnly) BACKGROUND_ONLY else FULL
    }

    data class Presets(
        val list: List<Preset>,
        val selected: Int,
    )

    fun of(scope: RoomScope): RoomFurni? =
        scope.room.furni
            .all()
            .firstOrNull { it.behaviour === dimmer }

    fun presets(scope: RoomScope): Presets {
        val room = part.of(scope.room)
        return room.presets ?: (store.load(scope.room.info.id) ?: DEFAULT).also { room.presets = it }
    }

    fun isOn(furni: RoomFurni): Boolean = furni.state.substringBefore(',') == ON

    @Suppress("LongParameterList") // one parameter per field of the message
    fun save(
        scope: RoomScope,
        playerId: Int,
        presetId: Int,
        type: Int,
        colour: String,
        brightness: Int,
        apply: Boolean,
    ) {
        if (!part.hooks.hasRights(scope, playerId) || presetId !in 1..PRESETS) return
        val onPalette = !part.host.checksDimmerColour() || colour.uppercase() in PALETTE
        val knownColour = HEX_COLOUR.matches(colour) && onPalette
        if (!knownColour || brightness !in MIN_LIGHT..MAX_LIGHT) return
        val furni = of(scope) ?: return
        val preset = Preset(presetId, type == BACKGROUND_ONLY, colour.uppercase(), brightness)
        val saved = Presets(presets(scope).list.map { if (it.id == presetId) preset else it }, presetId)
        part.of(scope.room).presets = saved
        val roomId = scope.room.info.id
        part.persist(Runnable { store.save(roomId, saved) })
        StateShow.data(scope, furni, state(if (apply) ON else OFF, preset))
        sendPresets(scope, playerId)
    }

    fun sendPresets(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        val furni = of(scope) ?: return
        val presets = presets(scope)
        val message = DimmerPresetsCodec.Presets(presets.selected, presets.list, isOn(furni), furni.id)
        scope.out.send(Lane.Now, Audience.Player(playerId), DimmerPresetsCodec, message)
    }

    fun switch(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (!part.hooks.hasRights(scope, playerId)) return
        val furni = of(scope) ?: return
        StateShow.data(scope, furni, switched(furni.state, scope))
    }

    private fun switched(
        state: String,
        scope: RoomScope,
    ): String {
        val fields = state.split(',')
        if (fields.size ==
            STATE_FIELDS
        ) {
            return (listOf(if (fields[0] == ON) OFF else ON) + fields.drop(1)).joinToString(",")
        }
        val presets = presets(scope)
        return state(ON, presets.list.firstOrNull { it.id == presets.selected } ?: presets.list.first())
    }

    companion object {
        private val HEX_COLOUR = Regex("#[0-9A-Fa-f]{6}")

        const val ON = "2"
        const val OFF = "1"
        private const val PRESETS = 3
        private const val FULL = 1
        private const val BACKGROUND_ONLY = 2
        private const val MIN_LIGHT = 76
        private const val MAX_LIGHT = 255
        private const val STATE_FIELDS = 5

        private val PALETTE = setOf("#74F5F5", "#0053F7", "#E759DE", "#EA4532", "#F2F851", "#82F349", "#000000")

        val DEFAULT =
            Presets(
                (1..PRESETS).map { Preset(it, backgroundOnly = false, colour = "#000000", brightness = MAX_LIGHT) },
                1,
            )

        fun state(
            on: String,
            preset: Preset,
        ): String = "$on,${preset.id},${preset.type},${preset.colour},${preset.brightness}"
    }
}
