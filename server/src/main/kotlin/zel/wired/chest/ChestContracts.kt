package zel.wired.chest

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource
import zel.wired.store.WiredWriter
import java.util.concurrent.ConcurrentHashMap

class ChestContracts(
    private val store: ChestStore,
    private val writer: WiredWriter,
    private val forms: () -> BoxSettingsSource?,
    private val types: ContractRecord.FurniTypes,
) : RoomPart {
    private val byId = ConcurrentHashMap<Int, Contract>()

    override fun load(room: Room) {
        val stored = store.roomContracts(room.info.id)
        room.furni.all().forEach { furni ->
            ContractKind.fromCode(Interactions.of(furni.definition))?.let { read(furni, it, stored[furni.id]) }
        }
    }

    override fun unload(scope: RoomScope) {
        scope.room.furni
            .all()
            .forEach { byId.remove(it.id) }
    }

    /** Called when a contract enters a room, so its row is known before anything writes over it. */
    fun placed(furni: RoomFurni) {
        val kind = ContractKind.fromCode(Interactions.of(furni.definition)) ?: return
        read(furni, kind, store.contract(furni.id))
    }

    fun pickedUp(furni: RoomFurni) {
        byId.remove(furni.id)
    }

    private fun read(
        furni: RoomFurni,
        kind: ContractKind,
        record: ContractRecord?,
    ) {
        byId[furni.id] = record?.toContract(kind, types) ?: Contract(kind)
    }

    fun of(
        room: Room,
        furni: RoomFurni,
    ): Contract? {
        val code = Interactions.of(furni.definition)
        return when {
            code == CustomContract.CODE -> {
                forms()?.settings(room, furni)?.let { CustomContract.terms(it.ints) }?.contract
            }

            else -> {
                ContractKind.fromCode(code)?.let { kind ->
                    byId.getOrPut(furni.id) { store.contract(furni.id)?.toContract(kind, types) ?: Contract(kind) }
                }
            }
        }
    }

    fun customIsUsable(
        room: Room,
        furni: RoomFurni,
    ): Boolean = forms()?.settings(room, furni)?.let { CustomContract.terms(it.ints).usable } == true

    fun chestsOf(
        room: Room,
        furni: RoomFurni,
    ): List<Int> =
        if (Interactions.of(furni.definition) == CustomContract.CODE) {
            forms()?.settings(room, furni)?.picks.orEmpty()
        } else {
            emptyList()
        }

    fun save(
        room: Room,
        furni: RoomFurni,
        contract: Contract,
    ) {
        byId[furni.id] = contract
        val record = ContractRecord.of(contract, types)
        val id = furni.id
        writer.write(room.info.id, Runnable { store.saveContract(id, record) })
    }
}
