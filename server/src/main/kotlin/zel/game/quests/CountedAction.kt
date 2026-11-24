package zel.game.quests

import java.util.Locale

enum class CountedAction {
    CREATE_ROOM,
    ENTER_OTHER_USERS_ROOM,
    CHAT_WITH_SOMEONE,
    ROOM_PULSE,
    TELEPORT,
    PLACE_ITEM,
    MOVE_ITEM,
    ROTATE_ITEM,
    SWITCH_ITEM_STATE,
    CLICK_FURNI,
    FIND_STUFF,
    WALK_OVER_STUFF,
    FIND_HAND_ITEM,
    PLACE_BUILD_PASS_FURNI,
    BUY_FROM_CATALOGUE,
    FRIEND_FURNI_LOCKED,

    FIND_PET,
    PET_RESPECT,
    PET_EAT,
    PET_LEVEL,
    PET_OTHER_USERS_ROOM,
    PLANT_MONSTER_PLANT,
    TREAT_MONSTER_PLANT,
    BREED_MONSTER_PLANT,

    GIVE_RESPECT,
    REPLENISH_RESPECT,
    REQUEST_FRIEND,
    FOLLOW_FRIEND,
    SEND_MESSENGER_MESSAGE,
    SEND_MESSENGER_INVITE,
    SET_RELATIONSHIP_STATUS,

    WAVE,
    DANCE,
    CHANGE_MOTTO,
    CHANGE_FIGURE,
    WEAR_BADGE,
    PUBLISH_PICTURE,
    USE_ICON,

    GAME_JOIN_GAME,
    TILECLAIM_LOCK_TILE,
    TILECLAIM_LOCK_AREA,
    TILECLAIM_PUSH_PUCK,
    TILECLAIM_PLAY_GAME,
    TILECLAIM_WIN_GAME,

    COMPLETE_QUEST,
    CLAIM_DAILY_TASK,
    ;

    companion object {
        private val BY_CODE: Map<String, CountedAction> = entries.associateBy { it.name }

        fun named(code: String?): CountedAction? {
            val wanted = code?.trim()?.uppercase(Locale.ROOT) ?: return null
            // every icon set lists the icon action under its own name
            return if (wanted.startsWith("USE_") && wanted.endsWith("ICON")) USE_ICON else BY_CODE[wanted]
        }
    }
}
