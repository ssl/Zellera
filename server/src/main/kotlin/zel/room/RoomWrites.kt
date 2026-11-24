package zel.room

/** The writes of one room run one after another, in the order handed in. */
fun interface RoomWrites {
    fun write(
        roomId: Int,
        job: Runnable,
    )
}
