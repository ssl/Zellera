package zel.game.moderation

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import java.sql.ResultSet
import javax.sql.DataSource

class ModerationCatalog(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)
    private val issueCategories = Int2ObjectOpenHashMap<IssueCategory>()
    private val textPresets = mapOf(USER_PRESETS to HashSet<String>(), ROOM_PRESETS to HashSet<String>())
    private val topicsById = HashMap<Int, ReportTopic>()

    val cfhCategories: List<ReportCategory>

    init {
        log.orFallback("moderation issue categories", Unit) { readIssueCategories() }
        log.orFallback("moderation text presets", Unit) { readTextPresets() }
        cfhCategories = log.orFallback("moderation report categories", emptyList()) { readCfhCategories() }
    }

    val issueCategoryNames: List<String> get() = issueCategories.values.map { it.name }
    val userPresets: List<String> get() = textPresets[USER_PRESETS].orEmpty().toList()
    val roomPresets: List<String> get() = textPresets[ROOM_PRESETS].orEmpty().toList()

    fun topic(topicId: Int): ReportTopic? = topicsById[topicId]

    private fun preset(id: Int): SanctionPreset? =
        if (id == 0) null else issueCategories.values.firstNotNullOfOrNull { it.presets[id] }

    private fun readIssueCategories() {
        sql.select("SELECT * FROM issue_categories").each { row ->
            issueCategories.put(row.getInt("id"), IssueCategory(row.getString("name").orEmpty()))
        }
        sql.select("SELECT * FROM issue_presets").each { row ->
            issueCategories[row.getInt("category_id")]?.add(SanctionPreset.fromRow(row))
        }
    }

    private fun readTextPresets() {
        sql.select("SELECT * FROM moderator_presets").each { row ->
            textPresets[row.getString("kind")]?.add(row.getString("text"))
        }
    }

    private fun readCfhCategories(): List<ReportCategory> {
        val topics =
            sql
                .select("SELECT * FROM report_topics ORDER BY category_id, sort_order, id")
                .rows { it.getInt("category_id") to topicOf(it) }
        topics.forEach { (_, topic) -> topicsById[topic.id] = topic }
        val byCategory = topics.groupBy({ it.first }, { it.second })
        return sql
            .select("SELECT id, internal_name FROM report_categories ORDER BY id")
            .rows { row ->
                ReportCategory(row.getString("internal_name").orEmpty(), byCategory[row.getInt("id")].orEmpty())
            }.filter { it.topics.isNotEmpty() }
    }

    private fun topicOf(row: ResultSet) =
        ReportTopic(
            id = row.getInt("id"),
            name = row.getString("internal_name").orEmpty(),
            goesTo = ReportDesk.ofStored(row.getString("goes_to")),
            reply = row.getString("reply").orEmpty(),
            replyWithoutLink = row.getBoolean("reply_without_link"),
            defaultSanction = preset(row.getInt("preset_id")),
            displayName = row.getString("display_name").orEmpty(),
            urgent = row.getBoolean("is_urgent"),
            ignoresAccused = row.getBoolean("ignores_accused"),
        )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ModerationCatalog::class.java)
        const val USER_PRESETS = "user"
        const val ROOM_PRESETS = "room"
    }
}
