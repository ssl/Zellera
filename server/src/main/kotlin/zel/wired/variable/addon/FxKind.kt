package zel.wired.variable.addon

@Suppress("MagicNumber") // the protocol's category, colour, width and renderer numbers
enum class FxKind(
    val code: String,
    val category: Int,
    val color: Int,
    val width: Int,
    val styles: List<FxStyle>,
    val hasRange: Boolean = true,
) {
    HEALTH(
        "wf_xtra_varfx_hp",
        0,
        1000,
        2,
        listOf(FxStyle(10, listOf("icon" to "misc_heart")), FxStyle(12), FxStyle(13), FxStyle(11)),
    ),
    PROGRESS("wf_xtra_varfx_prog", 1, 1, 2, listOf(FxStyle(0), FxStyle(2), FxStyle(3), FxStyle(4), FxStyle(1))),
    LEVELLING(
        "wf_xtra_varfx_levelling",
        2,
        1001,
        2,
        listOf(FxStyle(20, listOf("sub_renderer" to "2")), FxStyle(21, listOf("sub_renderer" to "1"))),
        hasRange = false,
    ),
    STATUS("wf_xtra_varfx_status", 3, -1, 2, STATUS_STYLES),
    BOSS(
        "wf_xtra_varfx_boss",
        4,
        5,
        4,
        listOf(FxStyle(100, listOf("icon" to "misc_skull", "icon_alignment" to "double")), FxStyle(100)),
    ),
    NUMBER(
        "wf_xtra_varfx_number",
        5,
        1,
        -1,
        listOf(
            FxStyle(201, listOf("design" to "freeze_style")),
            FxStyle(200, listOf("design" to "shalimar")),
            FxStyle(200, listOf("design" to "blocky")),
        ),
        hasRange = false,
    ),
    ;

    val renderer: Int get() = styles.first().renderer

    fun style(id: Int): Int = id.coerceIn(0, styles.lastIndex)

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: String): FxKind? = byCode[code]

        val codes: Set<String> = byCode.keys
    }
}

private fun status(
    color: String,
    icon: String,
    metallic: Boolean = false,
) = FxStyle(2, listOf("color" to color, "icon" to icon, "metallic" to metallic.toString()))

private val STATUS_STYLES: List<FxStyle> =
    listOf(
        status("#ffd83d", "energy"),
        status("#4aa9f6", "shield"),
        status("#8751d1", "magic"),
        status("#ff9f24", "food"),
        status("#86d213", "stamina"),
        status("#8ddc35", "poison"),
        status("#268fff", "mana"),
        status("#7dce35", "health"),
        status("#ffc83d", "gold", metallic = true),
        status("#416bdd", "gems", metallic = true),
        status("#fac384", "honor"),
        status("#ffd83d", "reputation"),
        status("#b8c3cc", "cooldown"),
        status("#74b9e8", "timeleft"),
        status("#ff5a1f", "burning"),
        status("#82cfff", "freezing"),
        FxStyle(2, listOf("icon" to "battery")),
        status("#c9c5b8", "repairing", metallic = true),
        status("#6254a8", "stealth"),
        status("#6bdc34", "upgrading"),
        status("#ffd900", "star_power", metallic = true),
        status("#4aabf5", "droplet"),
    )
