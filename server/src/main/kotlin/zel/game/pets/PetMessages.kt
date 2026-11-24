package zel.game.pets

import zel.net.clients.ClientConnection
import zel.protocol.generated.ConfirmBreedingRequestOut
import zel.protocol.generated.ConfirmBreedingResultOut
import zel.protocol.generated.GoToBreedingNestFailureOut
import zel.protocol.generated.NestBreedingSuccessOut
import zel.protocol.generated.OpenPetPackageRequestedOut
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PetBreedingResultOut
import zel.protocol.generated.PetCommandsOut
import zel.protocol.generated.PetLevelNotificationOut
import zel.protocol.generated.PetRespectNotificationOut
import zel.protocol.generated.PetSupplementedNotificationOut

object PetMessages {
    fun levelNotification(pet: Pet): PetLevelNotificationOut =
        PetLevelNotificationOut(
            int1 = pet.id,
            string1 = pet.name,
            int2 = pet.level,
            int3 = pet.kind?.type ?: -1,
            int4 = pet.race,
            string2 = pet.figureColor,
            int5 = pet.breedId,
            list1 = emptyList(),
        )

    fun supplemented(
        petId: Int,
        userId: Int,
        supplementType: Int,
    ): PetSupplementedNotificationOut = PetSupplementedNotificationOut(petId, userId, supplementType)

    fun commands(pet: Pet): PetCommandsOut {
        val all = pet.kind?.commands.orEmpty()
        return PetCommandsOut(
            int1 = pet.id,
            list1 = all.map { it.id },
            list2 = all.filter { pet.level >= it.level }.map { it.id },
        )
    }

    fun respect(pet: Pet): PetRespectNotificationOut {
        val parts =
            (pet as? HorsePet)?.figureParts.orEmpty().map {
                PetRespectNotificationOut.List1Item(it.slot, it.style, it.tint)
            }
        return PetRespectNotificationOut(
            int1 = pet.respect,
            int2 = pet.userId,
            int3 = pet.id,
            string1 = pet.name,
            int4 = pet.kind?.type ?: -1,
            int5 = pet.race,
            string2 = pet.figureColor,
            int6 = pet.breedId,
            list1 = parts,
            int7 = pet.level,
            int8 = if (pet.kind?.type == PetKind.MONSTERPLANT_TYPE) pet.breedId else -1,
        )
    }

    /** The result names the tier by its place in the rarest-first order, from 0. */
    fun nestSuccess(
        petId: Int,
        tier: BabyTier,
    ): NestBreedingSuccessOut = NestBreedingSuccessOut(petId, tier.ordinal)

    fun packageRequested(
        objectId: Int,
        petType: Int,
        palette: Int,
        color: String,
    ): OpenPetPackageRequestedOut =
        OpenPetPackageRequestedOut(
            objectId,
            OpenPetPackageRequestedOut.Trailing1(petType, palette, color, palette, emptyList()),
        )

    fun confirmResult(
        nestId: Int,
        result: Int,
    ): ConfirmBreedingResultOut = ConfirmBreedingResultOut(nestId, result)

    fun noWayToNest(reason: Int): GoToBreedingNestFailureOut = GoToBreedingNestFailureOut(reason)

    fun <T> send(
        client: ClientConnection,
        codec: OutgoingCodec<T>,
        message: T,
    ) = client.send(codec, message)

    fun plantBreedingResult(
        one: PlantSeed,
        two: PlantSeed,
    ): PetBreedingResultOut =
        PetBreedingResultOut(
            one.itemId,
            one.spriteId,
            one.itemName,
            one.userId,
            one.userName,
            one.rarity,
            one.mutation,
            two.itemId,
            two.spriteId,
            two.itemName,
            two.userId,
            two.userName,
            two.rarity,
            two.mutation,
        )

    /** What a full nest asks its owner: the parents, and per tier, rarest first, its chance and its breeds. */
    fun breedingConfirmation(
        nestId: Int,
        baby: BabyOdds,
        parentOne: BreedingParent,
        parentTwo: BreedingParent,
    ): ConfirmBreedingRequestOut {
        val tiers =
            BabyTier.entries.map { tier ->
                ConfirmBreedingRequestOut.List1Item(baby.chances[tier] ?: 0, baby.breeds[tier].orEmpty())
            }
        return ConfirmBreedingRequestOut(
            int1 = nestId,
            int2 = parentOne.pet.id,
            string1 = parentOne.pet.name,
            int3 = parentOne.pet.level,
            string2 = parentOne.pet.figureColor,
            string3 = parentOne.ownerName,
            int4 = parentTwo.pet.id,
            string4 = parentTwo.pet.name,
            int5 = parentTwo.pet.level,
            string5 = parentTwo.pet.figureColor,
            string6 = parentTwo.ownerName,
            list1 = tiers,
            int6 = baby.type,
        )
    }

    class BabyOdds(
        val type: Int,
        val chances: Map<BabyTier, Int>,
        val breeds: Map<BabyTier, List<Int>>,
    )

    class BreedingParent(
        val pet: Pet,
        ownerName: String?,
    ) {
        val ownerName: String = ownerName.orEmpty()
    }

    class PlantSeed(
        val itemId: Int,
        val spriteId: Int,
        val itemName: String,
        val userId: Int,
        val userName: String,
        val rarity: Int,
        val mutation: Boolean,
    ) {
        companion object {
            /** An empty slot; a slot whose item id is -1 is hidden. */
            val NONE = PlantSeed(-1, 0, "", 0, "", 0, false)
        }
    }
}
