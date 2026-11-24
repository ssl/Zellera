package zel.game.moderation

/** A folder of the moderator's preset list. */
class IssueCategory(
    val name: String,
) {
    private val byId = LinkedHashMap<Int, SanctionPreset>()

    val presets: Map<Int, SanctionPreset> get() = byId

    fun add(preset: SanctionPreset) {
        byId[preset.id] = preset
    }
}
