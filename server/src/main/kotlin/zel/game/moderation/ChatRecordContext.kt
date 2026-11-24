package zel.game.moderation

enum class ChatRecordContext(
    val key: String,
    val carries: Carries,
) {
    ROOM_NAME("roomName", Carries.TEXT),
    ROOM_ID("roomId", Carries.INT),
    GROUP_ID("groupId", Carries.INT),
    THREAD_ID("threadId", Carries.INT),
    MESSAGE_ID("messageId", Carries.INT),
    PHOTO_ID("extraDataId", Carries.TEXT),
    ;

    enum class Carries(
        val code: Byte,
    ) {
        INT(1),
        TEXT(2),
    }
}
