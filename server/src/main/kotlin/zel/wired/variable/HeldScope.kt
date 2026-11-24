package zel.wired.variable

import zel.protocol.generated.OutgoingCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomOutput

/** A scope whose output waits for [flush], for side effects that go out after the answer they belong to. */
internal class HeldScope(
    private val real: RoomScope,
) : RoomScope by real {
    private val held = ArrayList<() -> Unit>()

    override val out: RoomOutput =
        object : RoomOutput {
            override fun <T> send(
                lane: Lane,
                to: Audience,
                codec: OutgoingCodec<T>,
                message: T,
            ) {
                if (lane == Lane.NextTick) {
                    real.out.send(lane, to, codec, message)
                } else {
                    held += { real.out.send(lane, to, codec, message) }
                }
            }
        }

    fun flush() {
        held.forEach { it() }
        held.clear()
    }
}
