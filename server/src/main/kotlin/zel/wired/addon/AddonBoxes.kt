package zel.wired.addon

import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs
import kotlin.random.Random

class AddonBoxes(
    carrier: UserCarrier,
    names: UnitNames,
    amounts: FilterAmounts = FilterAmounts.NoVariables,
    random: Random = Random.Default,
    projectiles: Projectiles = Projectiles.None,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            type("wf_xtra_random") { id, s -> RandomEffects(id, s, random) },
            type("wf_xtra_unseen", ::UnseenEffect),
            type("wf_xtra_exec_in_order", ::ExecuteInOrder),
            type("wf_xtra_filter_furni") { id, s -> FilterFurni(id, s, amounts, random) },
            type("wf_xtra_filter_users") { id, s -> FilterUsers(id, s, amounts, random) },
            type("wf_xtra_anim_time", ::MoveAnimTime),
            type("wf_xtra_mov_no_animation", ::MoveNoAnimation),
            type("wf_xtra_mov_curve", ::MoveCurve),
            type("wf_xtra_mov_physics", ::MovePhysics),
            type("wf_xtra_mov_carry_users") { id, s -> MoveCarryUsers(id, s, carrier) },
            type("wf_xtra_rotate_to_dir") { id, s -> MoveRotateToDirection(id, s, projectiles) },
            type("wf_xtra_text_output_username") { id, s -> TextOutputUsername(id, s, names) },
            type("wf_xtra_text_output_furni_name", ::TextOutputFurniName),
            type(LinkSpeaker.CODE, ::LinkSpeaker),
            type(AchievementEnabler.CODE, ::AchievementEnabler),
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> {
        val row = requireNotNull(DialogSpecs.TABLE[code]) { "no dialog for $code" }
        return BoxType(code, BoxKind.Addon, row.form, create)
    }
}
