package zel.game.economy

import java.sql.SQLException

class UnknownLedgerPlayer(
    playerId: Int,
) : SQLException("No player $playerId for a ledger entry")
