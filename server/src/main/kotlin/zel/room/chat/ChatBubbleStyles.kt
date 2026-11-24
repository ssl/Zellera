package zel.room.chat

import zel.game.players.ChatStyles

/** The chat bubble styles the content defines; an id with no row is a custom shop style or the plain bubble. */
class ChatBubbleStyles {
    @Volatile private var known: Map<Int, ChatBubbleStyle> = emptyMap()

    fun of(id: Int): ChatBubbleStyle =
        known[id]
            ?: if (id in 1..ChatStyles.SHOP_STYLES.last) {
                ChatBubbleStyle(id, "CUSTOM_$id", "", overridable = true, triggersTalkingFurniture = false)
            } else {
                NORMAL
            }

    fun addAll(styles: List<ChatBubbleStyle>) {
        known = known + styles.associateBy { it.type }
    }

    companion object {
        val NORMAL = ChatBubbleStyle(0, "", "", overridable = true, triggersTalkingFurniture = true)
        val ALERT = ChatBubbleStyle(1, "", "", overridable = true, triggersTalkingFurniture = true)
    }
}
