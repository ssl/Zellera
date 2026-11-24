package zel.room.protocol

import zel.game.permissions.isAnyOwner
import zel.game.players.Player
import zel.game.quests.PlayerActions
import zel.protocol.bots.BotMessages
import zel.protocol.generated.BotCommandConfigurationOutCodec
import zel.protocol.generated.BreedPetsInCodec
import zel.protocol.generated.CancelPetBreedingInCodec
import zel.protocol.generated.CommandBotInCodec
import zel.protocol.generated.ConfirmPetBreedingInCodec
import zel.protocol.generated.GetBotCommandConfigurationDataInCodec
import zel.protocol.generated.GetPetCommandsInCodec
import zel.protocol.generated.GetPetInfoInCodec
import zel.protocol.generated.MountPetInCodec
import zel.protocol.generated.MovePetInCodec
import zel.protocol.generated.PassCarryItemToPetInCodec
import zel.protocol.generated.PetSelectedInCodec
import zel.protocol.generated.PlaceBotInCodec
import zel.protocol.generated.PlacePetInCodec
import zel.protocol.generated.PlaceRentableBotInCodec
import zel.protocol.generated.RemoveBotFromFlatInCodec
import zel.protocol.generated.RemovePetFromFlatInCodec
import zel.protocol.generated.RemoveSaddleFromPetInCodec
import zel.protocol.generated.RespectPetInCodec
import zel.protocol.generated.TogglePetBreedingPermissionInCodec
import zel.protocol.generated.TogglePetRidingPermissionInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.life.RoomLife
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.bot.BotSettings
import zel.room.unit.bot.RoomBots
import zel.room.unit.pet.PetCare
import zel.room.unit.pet.RoomPets
import kotlin.math.abs

class BotPetPackets(
    private val rooms: RoomRegistry,
    bots: RoomBots,
    settings: BotSettings,
    pets: RoomPets,
    care: PetCare,
    private val life: RoomLife,
) : PacketPackage {
    override val name = "bots-pets"
    override val routes =
        listOf(
            PacketRoute(PlaceBotInCodec) { session, m ->
                inRoom(session) { scope, player -> bots.place(scope, player, m.botId, Point(m.x, m.y)) }
            },
            PacketRoute(PlaceRentableBotInCodec) { session, m ->
                inRoom(session) { scope, player -> bots.move(scope, player, m.unitIndex, Point(m.x, m.y), m.direction) }
            },
            PacketRoute(RemoveBotFromFlatInCodec) { session, m ->
                inRoom(session) { scope, player -> bots.pickUp(scope, player, abs(m.botId)) }
            },
            PacketRoute(GetBotCommandConfigurationDataInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    val bot = bots.byId(scope, abs(m.botId))?.takeIf { mayConfigure(scope, player) } ?: return@inRoom
                    val answer = BotMessages.settings(bot.bot, m.skillType)
                    scope.out.send(
                        Lane.Now,
                        Audience.Player(player.profile.id),
                        BotCommandConfigurationOutCodec,
                        answer,
                    )
                }
            },
            PacketRoute(CommandBotInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    val bot = bots.byId(scope, abs(m.botId))?.takeIf { mayConfigure(scope, player) } ?: return@inRoom
                    settings.apply(scope, player, bot, m.skillType, m.data)
                }
            },
            PacketRoute(PlacePetInCodec) { session, m ->
                inRoom(session) { scope, player -> pets.place(scope, player, m.petId, Point(m.x, m.y)) }
            },
            PacketRoute(RemovePetFromFlatInCodec) { session, m ->
                inRoom(session) { scope, player -> pets.pickUp(scope, player, m.petId) }
            },
            PacketRoute(MovePetInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    if (mayConfigure(scope, player)) pets.move(scope, m.int1, Point(m.int2, m.int3), m.int4)
                }
            },
            PacketRoute(GetPetInfoInCodec) { session, m ->
                inRoom(session) { scope, player -> care.info(scope, player.profile.id, m.petId) }
            },
            PacketRoute(GetPetCommandsInCodec) { session, m ->
                inRoom(session) { scope, player -> care.commandList(scope, player.profile.id, m.petId) }
            },
            PacketRoute(PetSelectedInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    val type =
                        pets
                            .byId(scope, m.petId)
                            ?.pet
                            ?.kind
                            ?.type ?: return@inRoom
                    PlayerActions.chosePet(player, type)
                }
            },
            PacketRoute(RespectPetInCodec) { session, m ->
                inRoom(session) { scope, player -> care.scratch(scope, player, m.petId) }
            },
            PacketRoute(RemoveSaddleFromPetInCodec) { session, m ->
                inRoom(session) { scope, player -> pets.horses.removeSaddle(scope, player, m.petId) }
            },
            PacketRoute(PassCarryItemToPetInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    val unit = scope.room.units.ofPlayer(player.profile.id) ?: return@inRoom
                    care.takeHandItem(scope, unit, life, m.petId)
                }
            },
            PacketRoute(MountPetInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    val unit = scope.room.units.ofPlayer(player.profile.id) ?: return@inRoom
                    care.mount(scope, unit, player.profile.id, m.petId)
                }
            },
            PacketRoute(TogglePetRidingPermissionInCodec) { session, m ->
                inRoom(session) { scope, player -> care.toggleRiding(scope, player.profile.id, m.petId) }
            },
            PacketRoute(BreedPetsInCodec) { session, m ->
                if (!session.client.allow(BREED_RATE_GROUP, BREED_RATE_MILLIS)) return@PacketRoute
                inRoom(session) { scope, player ->
                    pets.breeding.breedPets(scope, player.profile.id, m.state, m.petOne, m.petTwo)
                }
            },
            PacketRoute(TogglePetBreedingPermissionInCodec) { session, m ->
                inRoom(session) { scope, player -> pets.breeding.togglePublic(scope, player.profile.id, m.petId) }
            },
            PacketRoute(ConfirmPetBreedingInCodec) { session, m ->
                inRoom(session) { scope, player ->
                    pets.nests.confirm(scope, player, m.itemId, m.name, m.petOneId, m.petTwoId)
                }
            },
            PacketRoute(CancelPetBreedingInCodec) { session, m ->
                inRoom(session) { scope, player -> pets.nests.cancel(scope, player, m.itemId) }
            },
        )

    private fun mayConfigure(
        scope: RoomScope,
        player: Player,
    ): Boolean = scope.room.info.ownerId == player.profile.id || player.isAnyOwner

    private fun inRoom(
        session: PacketSession,
        block: (RoomScope, Player) -> Unit,
    ) {
        val player = session.client.player ?: return
        rooms.withRoomOf(player.profile.id) { scope -> block(scope, player) }
    }

    private companion object {
        const val BREED_RATE_GROUP = "BreedPetsIn"
        const val BREED_RATE_MILLIS = 500L
    }
}
