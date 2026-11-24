package zel.room.side

import zel.protocol.generated.CompetitionEntrySubmitResultOut

/** What the entry window shows; [code] is the client's number for it. */
sealed interface EntryAnswer {
    val code: Int

    data object Entered : EntryAnswer {
        override val code = 0
    }

    data object Submit : EntryAnswer {
        override val code = 1
    }

    data object Confirm : EntryAnswer {
        override val code = 2
    }

    data class Missing(
        val furni: List<String>,
    ) : EntryAnswer {
        override val code = 3
    }

    data object DoorShut : EntryAnswer {
        override val code = 4
    }

    data object TooOld : EntryAnswer {
        override val code = 5
    }

    data object Rules : EntryAnswer {
        override val code = 6
    }

    fun toWire(competition: Competition): CompetitionEntrySubmitResultOut {
        val missing = (this as? Missing)?.furni.orEmpty()
        val required = if (this is Missing) competition.requiredFurni else emptyList()
        return CompetitionEntrySubmitResultOut(competition.id, competition.code, code, required, missing)
    }
}
