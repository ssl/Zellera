package zel.game.items

import java.sql.ResultSet

data class FurniDefinitionRow(
    val id: Int,
    val spriteId: Int,
    val code: String,
    val displayName: String,
    val kind: ProductKind,
    val width: Int,
    val length: Int,
    val stackHeight: Double,
    val canStack: Boolean,
    val canWalk: Boolean,
    val canSit: Boolean,
    val canLay: Boolean,
    val canRecycle: Boolean,
    val canTrade: Boolean,
    val canSell: Boolean,
    val canGift: Boolean,
    val stacksInInventory: Boolean,
    val interaction: ItemInteraction,
    val stateCount: Int,
    val effectMale: Int,
    val effectFemale: Int,
    val behaviourParams: String,
    val directions: FurniDirections,
    val handItems: IntArray,
    val stateHeights: DoubleArray,
    /** The usage policy this type sends (Objects/Items); null when the type has no fixed one. */
    val usagePolicy: Int? = null,
) {
    val isWebApiBox: Boolean
        get() = code.equals(WEB_API_BOX, ignoreCase = true) || interaction.name == WEB_API_BOX

    fun withWebApiRules(): FurniDefinitionRow =
        copy(canTrade = false, canSell = false, canRecycle = false, stacksInInventory = false)

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = id

    companion object {
        private const val DEFAULT_INTERACTION = "default"
        private const val WEB_API_BOX = "wf_xtra_var_web_api"

        fun read(
            set: ResultSet,
            interactionFor: (String) -> ItemInteraction,
        ): FurniDefinitionRow =
            FurniDefinitionRow(
                id = set.getInt("id"),
                spriteId = set.getInt("sprite_id"),
                code = set.getString("code").orEmpty(),
                displayName = set.getString("display_name").orEmpty(),
                kind = ProductKind.fromCode(set.getString("kind")),
                width = set.getShort("width").toInt(),
                length = set.getShort("length").toInt(),
                stackHeight = set.getDouble("stack_height"),
                canStack = set.getBoolean("can_stack"),
                canWalk = set.getBoolean("can_walk"),
                canSit = set.getBoolean("can_sit"),
                canLay = set.getBoolean("can_lay"),
                canRecycle = set.getBoolean("can_recycle"),
                canTrade = set.getBoolean("can_trade"),
                canSell = set.getBoolean("can_sell"),
                canGift = set.getBoolean("can_gift"),
                stacksInInventory = set.getBoolean("stacks_in_inventory"),
                interaction = interactionOf(set, interactionFor),
                stateCount = set.getShort("state_count").toInt(),
                effectMale = set.getShort("effect_male").toInt(),
                effectFemale = set.getShort("effect_female").toInt(),
                behaviourParams = set.getString("behaviour_params").orEmpty(),
                directions = FurniDirections(set.getInt("direction_mask")),
                handItems = handItems(set.getString("handitems")),
                stateHeights = stateHeights(set.getString("state_heights")),
                usagePolicy = (set.getObject("usage_policy") as? Number)?.toInt(),
            )

        internal fun handItems(text: String?): IntArray =
            text
                .orEmpty()
                .split(',', ';', '.')
                .mapNotNull { it.trim().toIntOrNull()?.takeIf { id -> id > 0 } }
                .toIntArray()

        internal fun stateHeights(text: String?): DoubleArray =
            text
                .orEmpty()
                .split(',')
                .mapNotNull { it.trim().toDoubleOrNull() }
                .toDoubleArray()

        private fun interactionOf(
            set: ResultSet,
            interactionFor: (String) -> ItemInteraction,
        ): ItemInteraction {
            val byBehaviour = interactionFor((set.getString("behaviour") ?: DEFAULT_INTERACTION).lowercase())
            val code = set.getString("code").orEmpty()
            val isPlain = byBehaviour.name.equals(DEFAULT_INTERACTION, ignoreCase = true)
            if (!isPlain || !code.startsWith("wf_", ignoreCase = true)) return byBehaviour
            val byName = interactionFor(code.lowercase())
            return if (byName.name.equals(DEFAULT_INTERACTION, ignoreCase = true)) byBehaviour else byName
        }
    }
}
