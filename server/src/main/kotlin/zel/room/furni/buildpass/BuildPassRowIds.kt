package zel.room.furni.buildpass

import zel.db.first
import zel.room.furni.BuildPass
import java.sql.Connection

object BuildPassRowIds {
    private const val ROW = "SELECT furni_id FROM build_pass_furni WHERE client_id = ?"

    const val JOIN: String = "LEFT JOIN build_pass_furni bc ON bc.furni_id = i.id"

    const val CLIENT_FACING: String = "COALESCE(bc.client_id, i.id)"

    fun rowOf(
        connection: Connection,
        id: Int,
    ): Int {
        if (!BuildPass.isClientId(id)) return id
        return connection.first(ROW, { int(id) }) { it.getInt(1) } ?: id
    }
}
