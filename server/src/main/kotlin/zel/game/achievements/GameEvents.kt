package zel.game.achievements

/**
 * What the game counts. The content pack hangs achievements on these names (`achievement_triggers`); a few carry a
 * detail after [DETAIL] (a pet type, a room type, a furni name) that a trigger may ask for.
 */
object GameEvents {
    const val DETAIL = ":"

    const val PET_LEVEL_UP = "pet.level_up"
    const val PET_SCRATCHED = "pet.scratched"
    const val PET_SCRATCHED_OWN = "pet.scratched_own"
    const val PET_FED = "pet.fed"
    const val PET_ACQUIRED = "pet.acquired"
    const val PET_BABY = "pet.baby"

    const val HORSE_RIDDEN_TILES = "horse.ridden_tiles"
    const val HORSE_JUMP = "horse.jump"
    const val HORSE_JUMP_STREAK = "horse.jump_streak"
    const val HORSE_JUMP_BY_GUEST = "horse.jump_by_guest"
    const val HORSE_RIDDEN_BY_GUEST = "horse.ridden_by_guest"

    const val PLANT_BRED = "plant.bred"
    const val PLANT_TREATED = "plant.treated"
    const val PLANT_REVIVED = "plant.revived"
    const val PLANT_DIED = "plant.died"
    const val PLANT_SHAPES_IN_ROOM = "plant.shapes_in_room"
    const val PLANT_COLOURS_IN_ROOM = "plant.colours_in_room"
    const val PLANT_NO_SEED = "plant.no_seed"

    const val QUEST_CAMPAIGN_COMPLETED = "quest.campaign_completed"

    const val ROOM_FURNI_COUNT = "room.furni_count"
    const val ROOM_FURNI_KINDS = "room.furni_kinds"
    const val ROOM_CREDIT_VALUE = "room.credit_value"
    const val ROOM_HOLE_FURNI = "room.hole_furni"
    const val ROOM_SLOPES = "room.slopes"
    const val ROOM_NOTE_LEFT = "room.note_left"
    const val ROOM_NOTE_RECEIVED = "room.note_received"
    const val ROOM_PAINTED = "room.painted"
    const val ROOM_HOSTING = "room.hosting"
    const val ROOM_BUILD_PASS_FURNI = "room.build_pass_furni"
    const val ROOM_ENTERED = "room.entered"
    const val ROOM_STAFF_PICK = "room.staff_pick"
    const val ROOM_COMPETITION_VOTE = "room.competition_vote"
    const val ROOM_KICKED_SOMEONE = "room.kicked_someone"
    const val ROOM_DOOR_MODE_SEEN = "room.door_mode_seen"
    const val ROOM_TIME = "room.time"

    const val FURNI_JUKEBOX_PLAYED = "furni.jukebox_played"
    const val FURNI_IGNITED = "furni.ignited"
    const val FURNI_EXERCISED = "furni.exercised"
    const val FURNI_SKATEBOARD_SLIDE = "furni.skateboard_slide"

    const val GAME_PLAYER_XP = "game.player_xp"
    const val GAME_AUTHOR_XP = "game.author_xp"
    const val GAME_FREEZE_WIN = "game.freeze.win"
    const val GAME_FREEZE_PLAY_SECONDS = "game.freeze.play_seconds"
    const val GAME_FREEZE_POWER_UP = "game.freeze.power_up"
    const val GAME_FREEZE_FROZE_OPPONENT = "game.freeze.froze_opponent"
    const val GAME_TILECLAIM_WIN = "game.tileclaim.win"
    const val GAME_TILECLAIM_PLAY_SECONDS = "game.tileclaim.play_seconds"
    const val GAME_TILECLAIM_TILE_LOCKED = "game.tileclaim.tile_locked"
    const val GAME_FOOTBALL_GOAL = "game.football.goal"
    const val GAME_FOOTBALL_GOAL_IN_ROOM = "game.football.goal_in_room"
    const val GAME_TAG_PLACED = "game.tag.placed"
    const val GAME_TAG_MINUTES = "game.tag.minutes"
    const val GAME_TAG_TAGGED = "game.tag.tagged"
    const val GAME_SNOWBALL_WIN = "game.snowball.win"
    const val GAME_SNOWBALL_HIT = "game.snowball.hit"
    const val GAME_SNOWBALL_TREE_HIT = "game.snowball.tree_hit"
    const val GAME_SNOWBALL_TOTAL_SCORE = "game.snowball.total_score"
    const val GAME_SNOWBALL_WEEKLY_TOP_TEN = "game.snowball.weekly_top_ten"
    const val GAME_SNOWBALL_WEEKLY_BEST = "game.snowball.weekly_best"

    const val PLAYER_LOGIN_STREAK = "player.login_streak"
    const val PLAYER_REGISTRATION_DAYS = "player.registration_days"
    const val PLAYER_MOTTO_CHANGED = "player.motto_changed"
    const val PLAYER_LOOK_CHANGED = "player.look_changed"
    const val PLAYER_NAME_CHANGED = "player.name_changed"
    const val PLAYER_FRIENDS = "player.friends"
    const val PLAYER_RESPECT_GIVEN = "player.respect_given"
    const val PLAYER_RESPECT_RECEIVED = "player.respect_received"

    const val CAMERA_PHOTO = "camera.photo"

    const val SAFETY_QUIZ_PASSED = "safety_quiz.passed"

    const val CHAT_STYLES_OWNED = "chat.styles_owned"

    const val EARNINGS_DAILY_GIFT = "earnings.daily_gift"

    const val CLUB_BUILD_PASS_DAYS = "club.build_pass_days"
    const val CLUB_DAYS = "club.days"

    const val CATALOG_GIFT_SENT = "catalog.gift_sent"
    const val CATALOG_GIFT_RECEIVED = "catalog.gift_received"
    const val CATALOG_LIMITED_BOUGHT = "catalog.limited_bought"
    const val CATALOG_LIMITED_EARLY = "catalog.limited_early"
    const val CATALOG_SONG_BOUGHT = "catalog.song_bought"

    const val RECYCLER_USED = "recycler.used"
    const val RECYCLER_ITEMS = "recycler.items"
    const val RECYCLER_ECO_PRIZE = "recycler.eco_prize"
    const val RECYCLER_RARE_PRIZE = "recycler.rare_prize"
    const val RECYCLER_GRAND_PRIZE = "recycler.grand_prize"
    const val RECYCLER_BOX_OPENED = "recycler.box_opened"

    const val CRAFTING_CRAFTED = "crafting.crafted"
    const val CRAFTING_SECRET_CRAFTED = "crafting.secret_crafted"

    const val DAILY_TASK_COMPLETED = "daily_task.completed"
    const val DAILY_TASK_STREAK = "daily_task.streak"

    const val REWARD_TRACK_COMPLETED = "reward_track.completed"

    const val TRADE_DONE = "trade.done"
    const val TRADE_COIN_VALUE = "trade.coin_value"

    const val CHEST_ORGANIZED = "chest.organized"
    const val CHEST_GIFT_GIVEN = "chest.gift_given"
    const val CHEST_COIN_SALE = "chest.coin_sale"

    /** [event] with [detail], for the triggers that ask for one. */
    fun of(
        event: String,
        detail: Any,
    ): String = "$event$DETAIL$detail"
}
