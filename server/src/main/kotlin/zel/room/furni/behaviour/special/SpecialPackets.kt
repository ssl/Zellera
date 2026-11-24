package zel.room.furni.behaviour.special

import zel.protocol.generated.CompostPlantInCodec
import zel.protocol.generated.CustomizePetWithFurniInCodec
import zel.protocol.generated.GiveSupplementToPetInCodec
import zel.protocol.generated.MysteryBoxWaitingCanceledInCodec
import zel.protocol.generated.OpenPetPackageInCodec
import zel.protocol.generated.RoomDimmerChangeStateInCodec
import zel.protocol.generated.RoomDimmerGetPresetsInCodec
import zel.protocol.generated.RoomDimmerSavePresetInCodec
import zel.protocol.generated.SetObjectDataInCodec
import zel.protocol.generated.SetRoomBackgroundColorDataInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope

class SpecialPackets(
    private val registry: RoomRegistry,
    part: SpecialPart,
) : PacketPackage {
    override val name = "furni-special"
    override val routes =
        listOf(
            PacketRoute(MysteryBoxWaitingCanceledInCodec) { session, _ ->
                inRoom(session) { scope, playerId -> part.mystery.cancel(scope, playerId) }
            },
            PacketRoute(SetObjectDataInCodec) { session, m ->
                inRoom(session) { scope, playerId ->
                    val furni = scope.room.furni[m.itemId]
                    val editable = furni?.behaviour as? ObjectDataEditable
                    val adsRefused = editable === part.ads && !part.host.mayEditAds(playerId)
                    if (furni != null && editable != null && !adsRefused && part.hooks.hasRights(scope, playerId)) {
                        editable.edit(scope, furni, pairs(m.keysAndValues))
                    }
                }
            },
            PacketRoute(SetRoomBackgroundColorDataInCodec) { session, m ->
                inRoom(session) { scope, playerId ->
                    val furni = scope.room.furni[m.int1]
                    if (furni != null && furni.behaviour === part.toner) {
                        part.toner.setColour(scope, furni, playerId, listOf(m.int2, m.int3, m.int4))
                    }
                }
            },
            PacketRoute(RoomDimmerGetPresetsInCodec) { session, _ ->
                inRoom(session) { scope, playerId -> part.dimmers.sendPresets(scope, playerId) }
            },
            PacketRoute(RoomDimmerSavePresetInCodec) { session, m ->
                inRoom(session) { scope, playerId ->
                    part.dimmers.save(scope, playerId, m.int1, m.int2, m.string1, m.int3, m.boolean1)
                }
            },
            PacketRoute(RoomDimmerChangeStateInCodec) { session, _ ->
                inRoom(session) { scope, playerId -> part.dimmers.switch(scope, playerId) }
            },
            PacketRoute(OpenPetPackageInCodec) { session, m ->
                inRoom(session) { scope, playerId -> part.petFurni.open(scope, playerId, m.itemId, m.name) }
            },
            PacketRoute(GiveSupplementToPetInCodec) { session, m ->
                inRoom(session) { scope, playerId -> part.pets.supplement(scope, playerId, m.petId, m.supplementType) }
            },
            PacketRoute(CustomizePetWithFurniInCodec) { session, m ->
                inRoom(session) { scope, playerId -> part.petFurni.useOnPet(scope, playerId, m.itemId, m.petId) }
            },
            PacketRoute(CompostPlantInCodec) { session, m ->
                inRoom(session) { scope, playerId -> part.petFurni.compost(scope, playerId, m.petId) }
            },
        )

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Int) -> Unit,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        registry.withPlayer(playerId) { scope, _ -> block(scope, playerId) }
    }

    private companion object {
        fun pairs(flat: List<String>): Map<String, String> =
            flat
                .chunked(2)
                .filter { it.size == 2 }
                .associate { (key, value) -> key to value }
    }
}
