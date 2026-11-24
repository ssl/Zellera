package zel.wired.selector

import zel.wired.WiredRooms
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveOutcome

class SelectorBoxes(
    rooms: () -> WiredRooms?,
    facts: SelectorFacts,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_slc_furni_picks", ::FurniPicks),
            type("wf_slc_furni_area", ::FurniArea),
            type("wf_slc_furni_bytype", ::FurniByType),
            type("wf_slc_furni_altitude", ::FurniAltitude),
            type("wf_slc_furni_onfurni", ::FurniOnFurni),
            type("wf_slc_furni_neighborhood", ::FurniNeighbourhood),
            type("wf_slc_furni_signal", ::FurniSignal),
            type("wf_slc_remote") { id, settings -> RemoteSelector(id, settings, rooms) },
            type("wf_slc_users_area", ::UserArea),
            type("wf_slc_users_byname") { id, settings -> UserByName(id, settings, facts) },
            type("wf_slc_users_bytype", ::UserByType),
            type("wf_slc_users_byaction") { id, settings -> UserByAction(id, settings, facts) },
            type("wf_slc_users_handitem") { id, settings -> UserHandItem(id, settings, facts) },
            type("wf_slc_users_neighborhood", ::UserNeighbourhood),
            type("wf_slc_users_onfurni", ::UserOnFurni),
            type("wf_slc_users_signal", ::UserSignal),
            type("wf_slc_users_team") { id, settings -> UserTeam(id, settings, facts) },
            type("wf_slc_users_group") { id, settings -> UserGroup(id, settings, facts) },
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> {
        val row = requireNotNull(DialogSpecs.TABLE[code]) { "no dialog for $code" }
        return BoxType(code, BoxKind.Selector, row.form, create)
    }

    companion object {
        val validations: Map<String, BoxValidation> =
            mapOf(
                "wf_slc_users_byaction" to
                    BoxValidation { _, settings ->
                        val action = settings.intAt(0)
                        val kept =
                            if (UserByAction.isAction(action)) settings else settings.copy(ints = listOf(0))
                        SaveOutcome.Saved(kept)
                    },
            )
    }
}
