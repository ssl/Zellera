package zel.wired.chest

import zel.protocol.generated.WiredOpenContractOut
import zel.protocol.generated.WiredOpenContractOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.protocol.BoxUse

class ContractFurni(
    private val use: BoxUse,
    private val contracts: ChestContracts,
) : Usable,
    Placeable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        use.onUse(scope, furni, playerId, param)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredOpenContractOutCodec,
            WiredOpenContractOut(furni.id),
        )
    }

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        contracts.placed(furni)
        use.onPlaced(scope, furni)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) = contracts.pickedUp(furni)
}
