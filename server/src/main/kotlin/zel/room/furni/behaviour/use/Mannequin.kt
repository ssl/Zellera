package zel.room.furni.behaviour.use

import zel.game.items.StateFields
import zel.game.players.Gender
import zel.game.players.Player
import zel.game.players.figure.Figure
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.StuffOnWire

class Mannequin(
    private val look: LookChanges,
    private val filter: (String, Player) -> String = { text, _ -> text },
) : Usable,
    StuffOnWire {
    interface LookChanges {
        fun genderOf(playerId: Int): Gender?

        fun lookOf(playerId: Int): String

        fun wear(
            scope: RoomScope,
            playerId: Int,
            look: String,
        )
    }

    private data class Outfit(
        val gender: String,
        val clothes: List<String>,
        val name: String,
    ) {
        fun state(): String =
            StateFields
                .write()
                .text(GENDER, gender)
                .text(LOOK, clothes.joinToString("."))
                .text(NAME, name)
                .state()
    }

    private fun outfitOf(furni: RoomFurni): Outfit? =
        StateFields.read(furni.state)?.let { Outfit(it.text(GENDER), clothesOf(furni, it.text(LOOK)), it.text(NAME)) }

    /** The parts of [look] a mannequin shows, the same ones the client swaps in its preview. */
    private fun clothesOf(
        furni: RoomFurni,
        look: String,
    ): List<String> = clothesOf(furni).let { shown -> look.split('.').filter { it.substringBefore('-') in shown } }

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val outfit = outfitOf(furni) ?: return
        if (outfit.clothes.isEmpty() || !outfit.gender.equals(look.genderOf(playerId)?.name, ignoreCase = true)) return
        val shown = clothesOf(furni)
        val own = look.lookOf(playerId).split('.').filter { it.isNotEmpty() && it.substringBefore('-') !in shown }
        val next = (own + outfit.clothes).joinToString(".")
        if (Figure.isWellFormed(next)) look.wear(scope, playerId, next)
    }

    override fun stuff(furni: RoomFurni): StuffData {
        val outfit = outfitOf(furni) ?: Outfit(UNDRESSED_GENDER, emptyList(), "")
        return SpecialStuff.map(
            furni,
            mapOf(
                "GENDER" to outfit.gender,
                "FIGURE" to outfit.clothes.joinToString("."),
                "OUTFIT_NAME" to outfit.name,
            ),
        )
    }

    fun rename(
        scope: RoomScope,
        player: Player,
        furni: RoomFurni,
        name: String,
    ) {
        if (name.length > MAX_NAME) return
        val outfit = outfitOf(furni) ?: Outfit(UNDRESSED_GENDER, emptyList(), "")
        StateShow.whole(scope, furni, outfit.copy(name = filter(name, player)).state())
    }

    fun dress(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
    ) {
        val gender = look.genderOf(playerId)?.name?.lowercase() ?: return
        val name = outfitOf(furni)?.name.orEmpty()
        StateShow.whole(scope, furni, Outfit(gender, clothesOf(furni, look.lookOf(playerId)), name).state())
    }

    companion object {
        private const val GENDER = "gender"
        private const val LOOK = "look"
        private const val NAME = "name"
        private const val UNDRESSED_GENDER = "m"
        private const val MAX_NAME = 50

        val CLOTHES = setOf("ca", "cc", "ch", "lg", "sh", "wa")

        /** The part types the mannequin's params name (`ca-cc-ch-...`), the standard six for one that names none. */
        fun clothesOf(furni: RoomFurni): Set<String> =
            furni.definition.params
                .split('-')
                .map(String::trim)
                .filter { it.length == PART_TYPE_LENGTH && it.all(Char::isLetter) }
                .toSet()
                .ifEmpty { CLOTHES }

        private const val PART_TYPE_LENGTH = 2
    }
}
