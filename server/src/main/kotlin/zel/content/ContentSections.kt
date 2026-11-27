package zel.content

/** In write order: a section whose room scope reads a table precedes that table's section. */
internal object ContentSections {
    const val FORMAT_VERSION = 5

    /** The oldest pack format this version still reads. */
    const val MIN_FORMAT = 5

    fun reads(format: Int): Boolean = format in MIN_FORMAT..FORMAT_VERSION

    fun supported(): String =
        if (MIN_FORMAT == FORMAT_VERSION) "format $FORMAT_VERSION" else "formats $MIN_FORMAT to $FORMAT_VERSION"

    val REQUIRED = setOf("furni/definitions", "catalogue/layouts", "catalogue/pages", "catalogue/items", "rooms/models")

    const val ROOM_LIST = "rooms/list"
    const val ROOM_INDEX_TABLE = "public_rooms"

    private const val ALL_ROWS = "TRUE"
    private const val FURNI_OF_ROOMS = "SELECT id FROM furni WHERE room_id IN (%s)"
    private const val ITEMS_OF_ROOMS = "furni_id IN ($FURNI_OF_ROOMS)"
    private const val IN_ROOMS = "room_id IN (%s)"
    private val PLAYER = IdSpace.PLAYER
    private val ROOM = IdSpace.ROOM
    private val GROUP = IdSpace.GROUP
    private val BOT = IdSpace.BOT
    private val FURNI = IdSpace.FURNI

    val ALL: List<TableSection> =
        listOf(
            TableSection("players/list", "players", ImportMode.PLAYERS, owns = PLAYER),
            TableSection("furni/definitions", "furni_definitions"),
            TableSection("furni/furni-handitems", "furni_handitems"),
            TableSection("furni/furni-state-heights", "furni_state_heights"),
            TableSection("furni/crackables", "crackables"),
            TableSection("furni/crackable-prizes", "crackable_prizes"),
            TableSection("furni/plants", "plant_kinds"),
            TableSection("furni/gift-wrappers", "gift_wrappers"),
            TableSection("furni/soundtracks", "soundtracks"),
            TableSection("furni/recycler-prizes", "recycler_prizes"),
            TableSection("furni/mystery-prizes", "mystery_box_prizes"),
            TableSection("furni/bot-serves", "bot_serve_keywords"),
            TableSection("furni/crafting-recipes", "crafting_recipes"),
            TableSection("furni/crafting-ingredients", "crafting_ingredients"),
            TableSection("catalogue/layouts", "catalogue_layouts"),
            TableSection("catalogue/pages", "catalogue_pages"),
            TableSection("catalogue/page-includes", "catalogue_page_includes"),
            TableSection("catalogue/items", "catalogue_offers", retired = setOf("rent_days")),
            TableSection("catalogue/preview-images", "offer_preview_images"),
            TableSection("catalogue/offer-items", "catalogue_offer_items"),
            TableSection("catalogue/build-pages", "catalogue_pages", keep = ALL_ROWS),
            TableSection("catalogue/build-items", "catalogue_offers", keep = ALL_ROWS, retired = setOf("rent_days")),
            TableSection("catalogue/build-offer-items", "catalogue_offer_items", keep = ALL_ROWS),
            TableSection(
                "catalogue/limited",
                "catalogue_limited_sales",
                keep = ALL_ROWS,
                skip = "offer_id IN (SELECT offer_id FROM catalogue_limited_sales)",
            ),
            TableSection("catalogue/clothing", "clothing"),
            TableSection("catalogue/clothing-sets", "clothing_sets"),
            TableSection("catalogue/club-offers", "club_offers"),
            TableSection("catalogue/featured-pages", "catalogue_featured_slots"),
            TableSection("catalogue/target-offers", "target_offers"),
            TableSection("catalogue/target-offer-vars", "target_offer_vars"),
            TableSection("catalogue/claimables", "claimable_products"),
            TableSection("catalogue/welcome-gifts", "welcome_gifts"),
            TableSection("catalogue/chat-bubbles", "chat_bubbles"),
            TableSection("catalogue/icon-collections", "icon_collections"),
            TableSection("catalogue/icons", "icons"),
            TableSection("catalogue/looks", "featured_looks"),
            TableSection("catalogue/calendar-campaigns", "calendar_campaigns"),
            TableSection("catalogue/calendar-prizes", "calendar_prizes"),
            TableSection("badges/group-parts", "guild_badge_parts"),
            TableSection("achievements/levels", "achievements"),
            TableSection("achievements/achievement-levels", "achievement_levels"),
            TableSection("achievements/triggers", "achievement_triggers"),
            TableSection("achievements/daily-tasks", "daily_tasks"),
            TableSection("achievements/quests", "quests", retired = setOf("seasonal_seconds")),
            TableSection("achievements/community-goals", "community_goals"),
            TableSection("achievements/community-goal-levels", "community_goal_levels"),
            TableSection("achievements/community-goal-options", "community_goal_options"),
            TableSection("achievements/track-definitions", "reward_tracks"),
            TableSection("achievements/track-levels", "reward_track_levels"),
            TableSection("achievements/track-prizes", "reward_track_prizes"),
            TableSection("achievements/track-tasks", "reward_track_tasks"),
            TableSection("pets/actions", "pet_types"),
            TableSection("pets/pet-type-actions", "pet_type_actions"),
            TableSection("pets/baby-breeds", "pet_baby_breeds"),
            TableSection("pets/palettes", "pet_palettes"),
            TableSection("pets/commands", "pet_type_commands"),
            TableSection("pets/command-data", "pet_commands"),
            TableSection("pets/vocals", "pet_vocals"),
            TableSection("pets/packages", "pet_packages"),
            TableSection("pets/items", "pet_furni"),
            TableSection("pets/boxes", "pet_boxes"),
            TableSection("navigator/room-categories", "room_categories", keep = "id = 1"),
            TableSection("navigator/public-categories", "public_room_categories"),
            TableSection("support/report-categories", "report_categories"),
            TableSection("support/report-topics", "report_topics"),
            TableSection("support/issue-categories", "issue_categories"),
            TableSection("support/issue-presets", "issue_presets"),
            TableSection("support/presets", "moderator_presets"),
            TableSection("news/articles", "news_articles", ImportMode.SEED),
            TableSection("news/panels", "news_panels", ImportMode.SEED),
            TableSection("misc/special-enables", "effect_rank_limits"),
            TableSection("misc/effect-roles", "effect_roles"),
            TableSection("misc/currencies", "currencies"),
            TableSection("misc/display-names", "display_names"),
            TableSection("misc/camera-filters", "camera_filters"),
            TableSection("misc/camera-libraries", "camera_libraries"),
            TableSection("rooms/models", "room_models", keep = "name = 'model_a'"),
            TableSection(
                "rooms/group-members",
                "guild_members",
                ImportMode.ROOMS,
                roomScope = "guild_id IN (SELECT id FROM guilds WHERE room_id IN (%s))",
                keep = "(guild_id, player_id) NOT IN (SELECT guild_id, player_id FROM %s)",
                refs = mapOf("guild_id" to GROUP, "player_id" to PLAYER),
            ),
            rooms("rooms/furni-labels", "furni_labels", ITEMS_OF_ROOMS, "furni_id" to FURNI),
            rooms(
                "rooms/teleport-pairs",
                "teleport_pairs",
                "first_furni_id IN ($FURNI_OF_ROOMS)",
                "first_furni_id" to FURNI,
                "second_furni_id" to FURNI,
            ),
            rooms(
                "rooms/build-pass-furni",
                "build_pass_furni",
                ITEMS_OF_ROOMS,
                "furni_id" to FURNI,
                "player_id" to PLAYER,
                "room_id" to ROOM,
            ),
            rooms(
                "rooms/wired-picks",
                "wired_box_picks",
                "box_id IN ($FURNI_OF_ROOMS)",
                "box_id" to FURNI,
                "furni_id" to FURNI,
            ),
            rooms(
                "rooms/wired-boxes",
                "wired_boxes",
                ITEMS_OF_ROOMS,
                "furni_id" to FURNI,
            ).copy(textRefs = setOf("variable_ids")),
            rooms("rooms/wired-chest-furni", "wired_chest_furni", "chest_id IN ($FURNI_OF_ROOMS)", "chest_id" to FURNI),
            rooms("rooms/wired-chests", "wired_chests", ITEMS_OF_ROOMS, "furni_id" to FURNI)
                .copy(live = setOf("furni_id", "credits")),
            rooms(
                "rooms/wired-contract-terms",
                "wired_contract_terms",
                "contract_id IN ($FURNI_OF_ROOMS)",
                "contract_id" to FURNI,
            ),
            rooms("rooms/wired-contracts", "wired_contracts", ITEMS_OF_ROOMS, "furni_id" to FURNI),
            rooms(
                "rooms/bot-chat-lines",
                "bot_chat_lines",
                "bot_id IN (SELECT id FROM bots WHERE room_id IN (%s))",
                "bot_id" to BOT,
            ),
            rooms("rooms/bots", "bots", IN_ROOMS, "player_id" to PLAYER, "room_id" to ROOM).copy(owns = BOT),
            rooms("rooms/furni", "furni", IN_ROOMS, "player_id" to PLAYER, "room_id" to ROOM, "guild_id" to GROUP)
                .copy(owns = FURNI, retired = setOf("rent_ends_at")),
            rooms("rooms/custom-models", "room_floor_plans", IN_ROOMS, "room_id" to ROOM),
            rooms("rooms/groups", "guilds", IN_ROOMS, "owner_id" to PLAYER, "room_id" to ROOM).copy(owns = GROUP),
            rooms(ROOM_LIST, "rooms", "id IN (%s)", "owner_id" to PLAYER, "guild_id" to GROUP)
                .copy(owns = ROOM, retired = setOf("roller_speed")),
            rooms("rooms/tags", "room_tags", IN_ROOMS, "room_id" to ROOM),
            rooms("rooms/dimmer-presets", "room_dimmer_presets", IN_ROOMS, "room_id" to ROOM),
            rooms("rooms/wired-settings", "wired_room_settings", IN_ROOMS, "room_id" to ROOM),
            rooms("rooms/wired-values", "wired_room_values", IN_ROOMS, "room_id" to ROOM, "variable_box_id" to FURNI),
            TableSection("rooms/index", ROOM_INDEX_TABLE, refs = mapOf("room_id" to ROOM)),
            TableSection("settings/values", "settings", ImportMode.MERGE),
            TableSection("settings/permissions", "permissions", ImportMode.MERGE),
            TableSection("settings/rank-permissions", "rank_permissions", ImportMode.MERGE),
            TableSection("texts/server", "texts", ImportMode.MERGE),
        )

    val BY_ID: Map<String, TableSection> = ALL.associateBy { it.id }

    val OWNERS: Map<IdSpace, TableSection> = ALL.mapNotNull { section -> section.owns?.let { it to section } }.toMap()

    private fun rooms(
        id: String,
        table: String,
        scope: String,
        vararg refs: Pair<String, IdSpace>,
    ) = TableSection(id, table, ImportMode.ROOMS, roomScope = scope, refs = refs.toMap())
}
