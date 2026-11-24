package zel.room.entry

import zel.game.players.Player
import zel.protocol.generated.OutgoingCodec

interface EntryEnvironment {
    fun player(id: Int): Player?

    /** Runs [job] off the room lane; jobs with an equal [key] run in the order given. */
    fun persistInOrder(
        key: Any,
        job: Runnable,
    )

    /** Wall-clock unix seconds. */
    fun nowSeconds(): Int

    fun landing(player: Player)

    /** A message to a player who is in no room's audience, such as one refused at a door. */
    fun <T> send(
        player: Player,
        codec: OutgoingCodec<T>,
        message: T,
    ) {}

    fun buildPassStatus(owner: Player) {}

    fun buildPassVisitDenied(
        owner: Player,
        visitor: String,
    ) {}

    fun countEntry(
        player: Player,
        firstVisit: Boolean,
    ) {}

    fun visitEnded(
        ownerId: Int,
        visitorLevel: Int,
        minutes: Int,
    ) = Unit

    fun entered(
        player: Player,
        roomId: Int,
    ) = Unit

    fun left(player: Player) = Unit
}
