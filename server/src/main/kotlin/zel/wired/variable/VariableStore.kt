package zel.wired.variable

import zel.wired.firing.HeldValue

/** Called on the persistence executor, or while a room loads. */
interface VariableStore : VariableLookups {
    fun roomValues(roomId: Int): StoredValues

    fun boxValues(
        roomId: Int,
        boxId: Int,
    ): StoredValues

    fun saveUser(
        roomId: Int,
        userId: Int,
        boxId: Int,
        value: Long?,
        held: HeldValue,
    )

    fun deleteUser(
        roomId: Int,
        userId: Int,
        boxId: Int,
    )

    fun saveFurni(
        roomId: Int,
        furniId: Int,
        boxId: Int,
        value: Long?,
        held: HeldValue,
    )

    fun deleteFurni(
        roomId: Int,
        furniId: Int,
        boxId: Int,
    )

    fun saveRoom(
        roomId: Int,
        boxId: Int,
        held: HeldValue,
    )

    fun deleteBox(
        roomId: Int,
        boxId: Int,
    )
}
