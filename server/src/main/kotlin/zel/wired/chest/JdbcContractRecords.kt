package zel.wired.chest

import zel.db.Params
import zel.db.first
import zel.db.rows
import zel.db.write
import zel.room.furni.buildpass.BuildPassRowIds
import zel.wired.chest.ChestRows.MAX_TEXT
import zel.wired.chest.ChestRows.ROW_ID
import zel.wired.chest.ChestRows.tiny
import zel.wired.store.writeRowByRow
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

internal class JdbcContractRecords(
    private val dataSource: DataSource,
) {
    fun ofRoom(roomId: Int): Map<Int, ContractRecord> =
        dataSource.connection.use { c ->
            val terms =
                c
                    .rows(ROOM_TERMS, { int(roomId) }) { it.getInt("id") to term(it) }
                    .groupBy({ it.first }, { it.second })
            c
                .rows(ROOM_CONTRACTS, { int(roomId) }) { rows ->
                    val id = rows.getInt("id")
                    id to record(rows, terms[id].orEmpty())
                }.toMap()
        }

    fun of(furniId: Int): ContractRecord? =
        dataSource.connection.use { c ->
            val clientId: Params.() -> Unit = {
                int(furniId)
                int(furniId)
            }
            val terms = c.rows(ONE_TERMS, clientId, ::term)
            c.first(ONE_CONTRACT, clientId) { record(it, terms) }
        }

    private fun record(
        rows: ResultSet,
        terms: List<ContractRecord.TermRow>,
    ) = ContractRecord(
        payment =
            PaymentForm(
                mode = rows.getInt("payment_mode"),
                receiveText = rows.getString("receive_text").orEmpty(),
                layout = rows.getString("layout") ?: Contract.LAYOUT_GENERIC,
            ),
        reward =
            RewardNotice(
                category = rows.getInt("reward_category"),
                popup = rows.getBoolean("show_reward_popup"),
                text = rows.getString("reward_text").orEmpty(),
            ),
        terms = terms,
    )

    private fun term(rows: ResultSet) =
        ContractRecord.TermRow(
            rows.getInt("side"),
            rows.getInt("rule"),
            rows.getInt("position"),
            rows.getBoolean("is_credits"),
            rows.getInt("amount"),
            rows.getInt("definition_id").takeUnless { rows.wasNull() },
            rows.getInt("unknown_sprite_id").takeUnless { rows.wasNull() },
            rows.getString("poster_code").orEmpty(),
        )

    /** Inside the caller's transaction. */
    fun save(
        c: Connection,
        furniId: Int,
        contract: ContractRecord,
    ) {
        val rowId = BuildPassRowIds.rowOf(c, furniId)
        val form = contract.payment
        val notice = contract.reward
        c.write(SAVE_CONTRACT) {
            int(tiny(form.mode))
            string(form.receiveText.take(MAX_TEXT))
            string(if (form.layout == Contract.LAYOUT_GAMES) Contract.LAYOUT_GAMES else Contract.LAYOUT_GENERIC)
            int(notice.category)
            boolean(notice.popup)
            string(notice.text.take(MAX_TEXT))
            int(rowId)
        }
        c.write(DELETE_TERMS) { int(rowId) }
        c.writeRowByRow(SAVE_TERM, contract.terms) { term ->
            int(term.side)
            int(term.rule)
            int(term.position)
            boolean(term.credits)
            int(term.amount)
            value(term.definitionId)
            value(term.unknownSpriteId)
            string(term.posterCode)
            int(rowId)
        }
    }

    private companion object {
        const val COLUMNS =
            "c.payment_mode, c.receive_text, c.layout, c.reward_category, c.show_reward_popup, c.reward_text"
        const val TERM_COLUMNS =
            "t.side, t.rule, t.position, t.is_credits, t.amount, t.definition_id, " +
                "t.unknown_sprite_id, t.poster_code"
        const val ROOM_CONTRACTS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS id, $COLUMNS FROM wired_contracts c " +
                "JOIN furni i ON i.id = c.furni_id ${BuildPassRowIds.JOIN} WHERE i.room_id = ?"
        const val ROOM_TERMS =
            "SELECT ${BuildPassRowIds.CLIENT_FACING} AS id, $TERM_COLUMNS FROM wired_contract_terms t " +
                "JOIN furni i ON i.id = t.contract_id ${BuildPassRowIds.JOIN} WHERE i.room_id = ?"
        const val ONE_CONTRACT = "SELECT $COLUMNS FROM wired_contracts c WHERE c.furni_id = $ROW_ID"
        const val ONE_TERMS = "SELECT $TERM_COLUMNS FROM wired_contract_terms t WHERE t.contract_id = $ROW_ID"
        const val SAVE_CONTRACT =
            "INSERT INTO wired_contracts (payment_mode, receive_text, layout, reward_category, show_reward_popup, " +
                "reward_text, furni_id) SELECT ?, ?, ?, ?, ?, ?, i.id FROM furni i WHERE i.id = ? " +
                "ON DUPLICATE KEY UPDATE payment_mode = VALUES(payment_mode), receive_text = VALUES(receive_text), " +
                "layout = VALUES(layout), reward_category = VALUES(reward_category), " +
                "show_reward_popup = VALUES(show_reward_popup), reward_text = VALUES(reward_text)"
        const val DELETE_TERMS = "DELETE FROM wired_contract_terms WHERE contract_id = ?"
        const val SAVE_TERM =
            "INSERT INTO wired_contract_terms (side, rule, position, is_credits, amount, definition_id, " +
                "unknown_sprite_id, poster_code, contract_id) SELECT ?, ?, ?, ?, ?, ?, ?, ?, c.furni_id FROM wired_contracts c WHERE c.furni_id = ?"
    }
}
