package zel.room.chat

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.Sql
import zel.db.SqlFailure

private val log = LoggerFactory.getLogger(ChatBubbleStore::class.java)

private const val ALL_BUBBLES_SQL =
    "SELECT id, name, permission_code, overridable, triggers_talking_furni FROM chat_bubbles ORDER BY id"

class ChatBubbleStore(
    private val database: Database,
) {
    fun readAll(): List<ChatBubbleStyle> =
        try {
            Sql(database.dataSource).select(ALL_BUBBLES_SQL).rows {
                ChatBubbleStyle(
                    it.getInt("id"),
                    it.getString("name"),
                    it.getString("permission_code"),
                    it.getBoolean("overridable"),
                    it.getBoolean("triggers_talking_furni"),
                )
            }
        } catch (e: SqlFailure) {
            log.error("chat bubble styles not read; only the free bubbles are known", e)
            emptyList()
        }
}
