package zel.protocol.generated

/** What one field of a client message may hold. */
sealed interface AllowedRule {
    data class Range(val min: Long, val max: Long) : AllowedRule

    data class Values(val values: Set<Any>) : AllowedRule

    /** A named server-side lookup; [whole] hands it a list field in one piece instead of item by item. */
    data class Lookup(val name: String, val whole: Boolean) : AllowedRule
}

/** One row of the table: the field's name in a reason, its rule, whether it holds an id and how to read it from [T]. */
class AllowedField<in T>(
    val label: String,
    val rule: AllowedRule,
    val holdsId: Boolean,
    val read: (T) -> Any?,
)

/** The allowed values of client message fields, by message name. */
object AllowedValueTable {
    private val rows: Map<String, List<AllowedField<*>>> = mapOf(
        "AcceptFriendIn" to listOf(
            AllowedField<AcceptFriendIn>("AcceptFriend.userIds", AllowedRule.Lookup("player", whole = false), true) { it.userIds },
        ),
        "AcceptQuestIn" to listOf(
            AllowedField<AcceptQuestIn>("AcceptQuest.questId", AllowedRule.Lookup("quest", whole = false), true) { it.questId },
        ),
        "ActivateQuestIn" to listOf(
            AllowedField<ActivateQuestIn>("ActivateQuest.questId", AllowedRule.Lookup("quest", whole = false), true) { it.questId },
        ),
        "AddAdminRightsToMemberIn" to listOf(
            AllowedField<AddAdminRightsToMemberIn>("AddAdminRightsToMember.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<AddAdminRightsToMemberIn>("AddAdminRightsToMember.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "AddFavouriteRoomIn" to listOf(
            AllowedField<AddFavouriteRoomIn>("AddFavouriteRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "AddItemToTradeIn" to listOf(
            AllowedField<AddItemToTradeIn>("AddItemToTrade.itemId", AllowedRule.Lookup("ownFurni", whole = false), true) { it.itemId },
        ),
        "AddJukeboxDiskIn" to listOf(
            AllowedField<AddJukeboxDiskIn>("AddJukeboxDisk.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "AddSpamWallPostItIn" to listOf(
            AllowedField<AddSpamWallPostItIn>("AddSpamWallPostIt.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<AddSpamWallPostItIn>("AddSpamWallPostIt.color", AllowedRule.Values(setOf("9CCEFF", "FF9CFF", "9CFF9C", "FFFF33", "FFFFFF", "FF9C9C", "FFCC66", "9CFFFF")), false) { it.color },
        ),
        "AmbassadorAlertIn" to listOf(
            AllowedField<AmbassadorAlertIn>("AmbassadorAlert.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "ApplySnapshotIn" to listOf(
            AllowedField<ApplySnapshotIn>("ApplySnapshot.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ApproveAllMembershipRequestsIn" to listOf(
            AllowedField<ApproveAllMembershipRequestsIn>("ApproveAllMembershipRequests.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "ApproveMembershipRequestIn" to listOf(
            AllowedField<ApproveMembershipRequestIn>("ApproveMembershipRequest.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<ApproveMembershipRequestIn>("ApproveMembershipRequest.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "AssignRightsIn" to listOf(
            AllowedField<AssignRightsIn>("AssignRights.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "PlayerExpressionIn" to listOf(
            AllowedField<PlayerExpressionIn>("PlayerExpression.int1", AllowedRule.Values(setOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 67L)), false) { it.int1 },
        ),
        "BanPlayerWithDurationIn" to listOf(
            AllowedField<BanPlayerWithDurationIn>("BanPlayerWithDuration.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<BanPlayerWithDurationIn>("BanPlayerWithDuration.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<BanPlayerWithDurationIn>("BanPlayerWithDuration.banName", AllowedRule.Values(setOf("RWUAM_BAN_USER_HOUR", "RWUAM_BAN_USER_DAY", "RWUAM_BAN_USER_PERM")), false) { it.banName },
        ),
        "BlockPlayerIn" to listOf(
            AllowedField<BlockPlayerIn>("BlockPlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "BreedPetsIn" to listOf(
            AllowedField<BreedPetsIn>("BreedPets.state", AllowedRule.Range(0L, 2L), false) { it.state },
        ),
        "BuildPassPlaceRoomItemIn" to listOf(
            AllowedField<BuildPassPlaceRoomItemIn>("BuildPassPlaceRoomItem.pageId", AllowedRule.Lookup("catalogPage", whole = false), true) { it.pageId },
            AllowedField<BuildPassPlaceRoomItemIn>("BuildPassPlaceRoomItem.offerId", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.offerId },
        ),
        "BuildPassPlaceWallItemIn" to listOf(
            AllowedField<BuildPassPlaceWallItemIn>("BuildPassPlaceWallItem.pageId", AllowedRule.Lookup("catalogPage", whole = false), true) { it.pageId },
            AllowedField<BuildPassPlaceWallItemIn>("BuildPassPlaceWallItem.offerId", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.offerId },
        ),
        "BuyMarketplaceOfferIn" to listOf(
            AllowedField<BuyMarketplaceOfferIn>("BuyMarketplaceOffer.offerId", AllowedRule.Lookup("marketplaceOffer", whole = false), true) { it.offerId },
        ),
        "CallForHelpFromIMIn" to listOf(
            AllowedField<CallForHelpFromIMIn>("CallForHelpFromIM.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<CallForHelpFromIMIn>("CallForHelpFromIM.topic", AllowedRule.Lookup("cfhTopic", whole = false), false) { it.topic },
        ),
        "CallForHelpFromPhotoIn" to listOf(
            AllowedField<CallForHelpFromPhotoIn>("CallForHelpFromPhoto.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<CallForHelpFromPhotoIn>("CallForHelpFromPhoto.topicId", AllowedRule.Lookup("cfhTopic", whole = false), false) { it.topicId },
        ),
        "CallForHelpIn" to listOf(
            AllowedField<CallForHelpIn>("CallForHelp.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<CallForHelpIn>("CallForHelp.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<CallForHelpIn>("CallForHelp.topic", AllowedRule.Lookup("cfhTopic", whole = false), false) { it.topic },
        ),
        "CancelMarketplaceOfferIn" to listOf(
            AllowedField<CancelMarketplaceOfferIn>("CancelMarketplaceOffer.offerId", AllowedRule.Lookup("marketplaceOffer", whole = false), true) { it.offerId },
        ),
        "CancelPetBreedingIn" to listOf(
            AllowedField<CancelPetBreedingIn>("CancelPetBreeding.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ChangePostureIn" to listOf(
            AllowedField<ChangePostureIn>("ChangePosture.posture", AllowedRule.Range(0L, 1L), false) { it.posture },
        ),
        "ChatIn" to listOf(
            AllowedField<ChatIn>("Chat.int1", AllowedRule.Range(0L, 10999L), false) { it.int1 },
        ),
        "ChatReviewGuideVoteIn" to listOf(
            AllowedField<ChatReviewGuideVoteIn>("ChatReviewGuideVote.voteType", AllowedRule.Range(0L, 2L), false) { it.voteType },
        ),
        "ChatReviewSessionCreateIn" to listOf(
            AllowedField<ChatReviewSessionCreateIn>("ChatReviewSessionCreate.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<ChatReviewSessionCreateIn>("ChatReviewSessionCreate.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "ClearMarketplaceOwnHistoryIn" to listOf(
            AllowedField<ClearMarketplaceOwnHistoryIn>("ClearMarketplaceOwnHistory.state", AllowedRule.Values(setOf(1L, 2L, 3L)), false) { it.state },
        ),
        "ClickFurniIn" to listOf(
            AllowedField<ClickFurniIn>("ClickFurni.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "CloseChestIn" to listOf(
            AllowedField<CloseChestIn>("CloseChest.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "CommandBotIn" to listOf(
            AllowedField<CommandBotIn>("CommandBot.botId", AllowedRule.Lookup("bot", whole = false), true) { it.botId },
            AllowedField<CommandBotIn>("CommandBot.skillType", AllowedRule.Values(setOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L, 12L, 14L, 24L, 25L)), false) { it.skillType },
        ),
        "CompostPlantIn" to listOf(
            AllowedField<CompostPlantIn>("CompostPlant.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "ConfirmPetBreedingIn" to listOf(
            AllowedField<ConfirmPetBreedingIn>("ConfirmPetBreeding.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ControlYoutubeDisplayPlaybackIn" to listOf(
            AllowedField<ControlYoutubeDisplayPlaybackIn>("ControlYoutubeDisplayPlayback.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "CreateFlatIn" to listOf(
            AllowedField<CreateFlatIn>("CreateFlat.modelName", AllowedRule.Lookup("roomModel", whole = false), false) { it.modelName },
            AllowedField<CreateFlatIn>("CreateFlat.categoryId", AllowedRule.Lookup("roomCategory", whole = false), false) { it.categoryId },
            AllowedField<CreateFlatIn>("CreateFlat.maxUsers", AllowedRule.Range(10L, 75L), false) { it.maxUsers },
            AllowedField<CreateFlatIn>("CreateFlat.tradeType", AllowedRule.Range(0L, 2L), false) { it.tradeType },
        ),
        "CreateGuildIn" to listOf(
            AllowedField<CreateGuildIn>("CreateGuild.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<CreateGuildIn>("CreateGuild.colorOne", AllowedRule.Lookup("guildSymbolColour", whole = false), false) { it.colorOne },
            AllowedField<CreateGuildIn>("CreateGuild.colorTwo", AllowedRule.Lookup("guildBackgroundColour", whole = false), false) { it.colorTwo },
            AllowedField<CreateGuildIn>("CreateGuild.parts", AllowedRule.Lookup("guildBadge", whole = true), false) { it.parts },
        ),
        "CreditFurniRedeemIn" to listOf(
            AllowedField<CreditFurniRedeemIn>("CreditFurniRedeem.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "CustomizePlayerWithFurniIn" to listOf(
            AllowedField<CustomizePlayerWithFurniIn>("CustomizePlayerWithFurni.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "CustomizePetWithFurniIn" to listOf(
            AllowedField<CustomizePetWithFurniIn>("CustomizePetWithFurni.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<CustomizePetWithFurniIn>("CustomizePetWithFurni.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "DanceIn" to listOf(
            AllowedField<DanceIn>("Dance.danceId", AllowedRule.Range(0L, 4L), false) { it.danceId },
        ),
        "DeactivateGuildIn" to listOf(
            AllowedField<DeactivateGuildIn>("DeactivateGuild.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "DeclineFriendIn" to listOf(
            AllowedField<DeclineFriendIn>("DeclineFriend.userIds", AllowedRule.Lookup("player", whole = false), true) { it.userIds },
        ),
        "DeleteFavouriteRoomIn" to listOf(
            AllowedField<DeleteFavouriteRoomIn>("DeleteFavouriteRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "DeleteRoomIn" to listOf(
            AllowedField<DeleteRoomIn>("DeleteRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "DeselectFavouriteGroupIn" to listOf(
            AllowedField<DeselectFavouriteGroupIn>("DeselectFavouriteGroup.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "DiceOffIn" to listOf(
            AllowedField<DiceOffIn>("DiceOff.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "EnterOneWayDoorIn" to listOf(
            AllowedField<EnterOneWayDoorIn>("EnterOneWayDoor.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ExtendRentOrBuyoutFurniIn" to listOf(
            AllowedField<ExtendRentOrBuyoutFurniIn>("ExtendRentOrBuyoutFurni.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ForwardToACompetitionRoomIn" to listOf(
            AllowedField<ForwardToACompetitionRoomIn>("ForwardToACompetitionRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "FriendFurniConfirmLockIn" to listOf(
            AllowedField<FriendFurniConfirmLockIn>("FriendFurniConfirmLock.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "FurniRecolorIn" to listOf(
            AllowedField<FurniRecolorIn>("FurniRecolor.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "GetBannedPlayersFromRoomIn" to listOf(
            AllowedField<GetBannedPlayersFromRoomIn>("GetBannedPlayersFromRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "GetBotCommandConfigurationDataIn" to listOf(
            AllowedField<GetBotCommandConfigurationDataIn>("GetBotCommandConfigurationData.botId", AllowedRule.Lookup("bot", whole = false), true) { it.botId },
            AllowedField<GetBotCommandConfigurationDataIn>("GetBotCommandConfigurationData.skillType", AllowedRule.Values(setOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L, 12L, 14L, 24L, 25L)), false) { it.skillType },
        ),
        "GetCatalogIndexIn" to listOf(
            AllowedField<GetCatalogIndexIn>("GetCatalogIndex.mODE", AllowedRule.Values(setOf("NORMAL", "BUILD_PASS")), false) { it.mODE },
        ),
        "GetCatalogPageIn" to listOf(
            AllowedField<GetCatalogPageIn>("GetCatalogPage.catalogPageId", AllowedRule.Lookup("catalogPage", whole = false), true) { it.catalogPageId },
            AllowedField<GetCatalogPageIn>("GetCatalogPage.offerId", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.offerId },
            AllowedField<GetCatalogPageIn>("GetCatalogPage.mode", AllowedRule.Values(setOf("NORMAL", "BUILD_PASS")), false) { it.mode },
        ),
        "GetCraftableProductsIn" to listOf(
            AllowedField<GetCraftableProductsIn>("GetCraftableProducts.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "GetCustomRoomFilterIn" to listOf(
            AllowedField<GetCustomRoomFilterIn>("GetCustomRoomFilter.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "GetExtendedProfileIn" to listOf(
            AllowedField<GetExtendedProfileIn>("GetExtendedProfile.playerId", AllowedRule.Lookup("player", whole = false), true) { it.playerId },
        ),
        "GetFlatControllersIn" to listOf(
            AllowedField<GetFlatControllersIn>("GetFlatControllers.requestedRoomId", AllowedRule.Lookup("room", whole = false), true) { it.requestedRoomId },
        ),
        "GetForumStatsIn" to listOf(
            AllowedField<GetForumStatsIn>("GetForumStats.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "GetForumsListIn" to listOf(
            AllowedField<GetForumsListIn>("GetForumsList.mode", AllowedRule.Range(0L, 2L), false) { it.mode },
            AllowedField<GetForumsListIn>("GetForumsList.int1", AllowedRule.Values(setOf(20L)), false) { it.int1 },
        ),
        "GetGuestRoomIn" to listOf(
            AllowedField<GetGuestRoomIn>("GetGuestRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<GetGuestRoomIn>("GetGuestRoom.isLoading", AllowedRule.Values(setOf(0L, 1L)), false) { it.isLoading },
            AllowedField<GetGuestRoomIn>("GetGuestRoom.checkEntry", AllowedRule.Values(setOf(0L, 1L)), false) { it.checkEntry },
        ),
        "GetGuildEditInfoIn" to listOf(
            AllowedField<GetGuildEditInfoIn>("GetGuildEditInfo.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "GetGuildFurniContextMenuInfoIn" to listOf(
            AllowedField<GetGuildFurniContextMenuInfoIn>("GetGuildFurniContextMenuInfo.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<GetGuildFurniContextMenuInfoIn>("GetGuildFurniContextMenuInfo.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "GetGuildMembersIn" to listOf(
            AllowedField<GetGuildMembersIn>("GetGuildMembers.groupId", AllowedRule.Lookup("guild", whole = false), true) { it.groupId },
            AllowedField<GetGuildMembersIn>("GetGuildMembers.levelId", AllowedRule.Range(0L, 2L), false) { it.levelId },
        ),
        "GetGroupDetailsIn" to listOf(
            AllowedField<GetGroupDetailsIn>("GetGroupDetails.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "GetIsOfferGiftableIn" to listOf(
            AllowedField<GetIsOfferGiftableIn>("GetIsOfferGiftable.offerId", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.offerId },
        ),
        "GetItemDataIn" to listOf(
            AllowedField<GetItemDataIn>("GetItemData.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "GetMarketplaceItemStatsIn" to listOf(
            AllowedField<GetMarketplaceItemStatsIn>("GetMarketplaceItemStats.int1", AllowedRule.Range(1L, 3L), false) { it.int1 },
        ),
        "GetMarketplaceOffersIn" to listOf(
            AllowedField<GetMarketplaceOffersIn>("GetMarketplaceOffers.min", AllowedRule.Range(-1L, 1000000000L), false) { it.min },
            AllowedField<GetMarketplaceOffersIn>("GetMarketplaceOffers.max", AllowedRule.Range(-1L, 1000000000L), false) { it.max },
            AllowedField<GetMarketplaceOffersIn>("GetMarketplaceOffers.type", AllowedRule.Range(1L, 6L), false) { it.type },
        ),
        "GetMarketplaceOwnOffersIn" to listOf(
            AllowedField<GetMarketplaceOwnOffersIn>("GetMarketplaceOwnOffers.int1", AllowedRule.Values(setOf(1L, 2L, 3L)), false) { it.int1 },
        ),
        "GetMemberGuildItemCountIn" to listOf(
            AllowedField<GetMemberGuildItemCountIn>("GetMemberGuildItemCount.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<GetMemberGuildItemCountIn>("GetMemberGuildItemCount.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "GetMessagesIn" to listOf(
            AllowedField<GetMessagesIn>("GetMessages.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<GetMessagesIn>("GetMessages.limit", AllowedRule.Values(setOf(20L)), false) { it.limit },
        ),
        "GetPetCommandsIn" to listOf(
            AllowedField<GetPetCommandsIn>("GetPetCommands.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "GetPetInfoIn" to listOf(
            AllowedField<GetPetInfoIn>("GetPetInfo.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "GetRaidProtectionSettingsIn" to listOf(
            AllowedField<GetRaidProtectionSettingsIn>("GetRaidProtectionSettings.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "GetRelationshipStatusInfoIn" to listOf(
            AllowedField<GetRelationshipStatusInfoIn>("GetRelationshipStatusInfo.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "GetResolutionAchievementsIn" to listOf(
            AllowedField<GetResolutionAchievementsIn>("GetResolutionAchievements.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "GetRoomChatlogIn" to listOf(
            AllowedField<GetRoomChatlogIn>("GetRoomChatlog.int1", AllowedRule.Values(setOf(0L, 1L)), false) { it.int1 },
        ),
        "GetRoomSettingsIn" to listOf(
            AllowedField<GetRoomSettingsIn>("GetRoomSettings.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "GetSelectedBadgesIn" to listOf(
            AllowedField<GetSelectedBadgesIn>("GetSelectedBadges.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "GetThreadIn" to listOf(
            AllowedField<GetThreadIn>("GetThread.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "GetThreadsIn" to listOf(
            AllowedField<GetThreadsIn>("GetThreads.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<GetThreadsIn>("GetThreads.int1", AllowedRule.Values(setOf(20L)), false) { it.int1 },
        ),
        "GetYoutubeDisplayStatusIn" to listOf(
            AllowedField<GetYoutubeDisplayStatusIn>("GetYoutubeDisplayStatus.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "GiveSupplementToPetIn" to listOf(
            AllowedField<GiveSupplementToPetIn>("GiveSupplementToPet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
            AllowedField<GiveSupplementToPetIn>("GiveSupplementToPet.supplementType", AllowedRule.Values(setOf(0L, 1L)), false) { it.supplementType },
        ),
        "IgnorePlayerIn" to listOf(
            AllowedField<IgnorePlayerIn>("IgnorePlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "JoinGroupIn" to listOf(
            AllowedField<JoinGroupIn>("JoinGroup.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "KickMemberIn" to listOf(
            AllowedField<KickMemberIn>("KickMember.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<KickMemberIn>("KickMember.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "KickPlayerIn" to listOf(
            AllowedField<KickPlayerIn>("KickPlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "MakeOfferIn" to listOf(
            AllowedField<MakeOfferIn>("MakeOffer.furniType", AllowedRule.Values(setOf(1L, 2L)), false) { it.furniType },
            AllowedField<MakeOfferIn>("MakeOffer.price", AllowedRule.Range(1L, 1000000000L), false) { it.price },
        ),
        "ModerateMessageIn" to listOf(
            AllowedField<ModerateMessageIn>("ModerateMessage.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<ModerateMessageIn>("ModerateMessage.state", AllowedRule.Values(setOf(1L, 10L, 20L)), false) { it.state },
        ),
        "ModerateRoomIn" to listOf(
            AllowedField<ModerateRoomIn>("ModerateRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<ModerateRoomIn>("ModerateRoom.lockDoor", AllowedRule.Values(setOf(0L, 1L)), false) { it.lockDoor },
            AllowedField<ModerateRoomIn>("ModerateRoom.changeTitle", AllowedRule.Values(setOf(0L, 1L)), false) { it.changeTitle },
            AllowedField<ModerateRoomIn>("ModerateRoom.kickUsers", AllowedRule.Values(setOf(0L, 1L)), false) { it.kickUsers },
        ),
        "ModerateThreadIn" to listOf(
            AllowedField<ModerateThreadIn>("ModerateThread.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<ModerateThreadIn>("ModerateThread.state", AllowedRule.Values(setOf(1L, 10L, 20L)), false) { it.state },
        ),
        "MountPetIn" to listOf(
            AllowedField<MountPetIn>("MountPet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "MoveObjectIn" to listOf(
            AllowedField<MoveObjectIn>("MoveObject.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
            AllowedField<MoveObjectIn>("MoveObject.rotation", AllowedRule.Range(0L, 7L), false) { it.rotation },
        ),
        "MoveWallItemIn" to listOf(
            AllowedField<MoveWallItemIn>("MoveWallItem.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "MutePlayerIn" to listOf(
            AllowedField<MutePlayerIn>("MutePlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<MutePlayerIn>("MutePlayer.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<MutePlayerIn>("MutePlayer.minutes", AllowedRule.Values(setOf(2L, 5L, 10L, 15L, 60L, 1080L, 2160L, 4320L)), false) { it.minutes },
        ),
        "NavigatorSetSearchCodeViewModeIn" to listOf(
            AllowedField<NavigatorSetSearchCodeViewModeIn>("NavigatorSetSearchCodeViewMode.viewMode", AllowedRule.Values(setOf(0L, 1L)), false) { it.viewMode },
        ),
        "OpenChestAndGetContentsIn" to listOf(
            AllowedField<OpenChestAndGetContentsIn>("OpenChestAndGetContents.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "OpenIn" to listOf(
            AllowedField<OpenIn>("Open.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "OpenMysteryTrophyIn" to listOf(
            AllowedField<OpenMysteryTrophyIn>("OpenMysteryTrophy.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "OpenPetPackageIn" to listOf(
            AllowedField<OpenPetPackageIn>("OpenPetPackage.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "OpenTradingIn" to listOf(
            AllowedField<OpenTradingIn>("OpenTrading.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "PassCarryItemIn" to listOf(
            AllowedField<PassCarryItemIn>("PassCarryItem.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "PassCarryItemToPetIn" to listOf(
            AllowedField<PassCarryItemToPetIn>("PassCarryItemToPet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "PetSelectedIn" to listOf(
            AllowedField<PetSelectedIn>("PetSelected.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "PickupObjectIn" to listOf(
            AllowedField<PickupObjectIn>("PickupObject.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<PickupObjectIn>("PickupObject.category", AllowedRule.Values(setOf(1L, 2L)), false) { it.category },
        ),
        "PlaceBotIn" to listOf(
            AllowedField<PlaceBotIn>("PlaceBot.botId", AllowedRule.Lookup("bot", whole = false), true) { it.botId },
        ),
        "PlacePetIn" to listOf(
            AllowedField<PlacePetIn>("PlacePet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "PlacePostItIn" to listOf(
            AllowedField<PlacePostItIn>("PlacePostIt.itemId", AllowedRule.Lookup("ownFurni", whole = false), true) { it.itemId },
        ),
        "PollAnswerIn" to listOf(
            AllowedField<PollAnswerIn>("PollAnswer.pollId", AllowedRule.Lookup("poll", whole = false), true) { it.pollId },
        ),
        "PollRejectIn" to listOf(
            AllowedField<PollRejectIn>("PollReject.pollId", AllowedRule.Lookup("poll", whole = false), true) { it.pollId },
        ),
        "PollStartIn" to listOf(
            AllowedField<PollStartIn>("PollStart.pollId", AllowedRule.Lookup("poll", whole = false), true) { it.pollId },
        ),
        "PostMessageIn" to listOf(
            AllowedField<PostMessageIn>("PostMessage.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "PurchaseFromCatalogAsGiftIn" to listOf(
            AllowedField<PurchaseFromCatalogAsGiftIn>("PurchaseFromCatalogAsGift.int3", AllowedRule.Lookup("giftSprite", whole = false), false) { it.int3 },
            AllowedField<PurchaseFromCatalogAsGiftIn>("PurchaseFromCatalogAsGift.int4", AllowedRule.Lookup("giftBox", whole = false), false) { it.int4 },
            AllowedField<PurchaseFromCatalogAsGiftIn>("PurchaseFromCatalogAsGift.int5", AllowedRule.Lookup("giftRibbon", whole = false), false) { it.int5 },
            AllowedField<PurchaseFromCatalogAsGiftIn>("PurchaseFromCatalogAsGift.int1", AllowedRule.Lookup("catalogPage", whole = false), true) { it.int1 },
            AllowedField<PurchaseFromCatalogAsGiftIn>("PurchaseFromCatalogAsGift.int2", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.int2 },
        ),
        "PurchaseFromCatalogIn" to listOf(
            AllowedField<PurchaseFromCatalogIn>("PurchaseFromCatalog.int3", AllowedRule.Lookup("catalogAmount", whole = false), false) { it.int3 },
            AllowedField<PurchaseFromCatalogIn>("PurchaseFromCatalog.int1", AllowedRule.Lookup("catalogPage", whole = false), true) { it.int1 },
            AllowedField<PurchaseFromCatalogIn>("PurchaseFromCatalog.int2", AllowedRule.Lookup("catalogOffer", whole = false), true) { it.int2 },
        ),
        "PurchaseRoomAdIn" to listOf(
            AllowedField<PurchaseRoomAdIn>("PurchaseRoomAd.pageId", AllowedRule.Lookup("catalogPage", whole = false), true) { it.pageId },
            AllowedField<PurchaseRoomAdIn>("PurchaseRoomAd.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "RateFlatIn" to listOf(
            AllowedField<RateFlatIn>("RateFlat.points", AllowedRule.Values(setOf(1L)), false) { it.points },
        ),
        "RejectMembershipRequestIn" to listOf(
            AllowedField<RejectMembershipRequestIn>("RejectMembershipRequest.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<RejectMembershipRequestIn>("RejectMembershipRequest.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "RejectQuestIn" to listOf(
            AllowedField<RejectQuestIn>("RejectQuest.questId", AllowedRule.Lookup("quest", whole = false), true) { it.questId },
        ),
        "RemoveAdminRightsFromMemberIn" to listOf(
            AllowedField<RemoveAdminRightsFromMemberIn>("RemoveAdminRightsFromMember.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<RemoveAdminRightsFromMemberIn>("RemoveAdminRightsFromMember.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "RemoveBotFromFlatIn" to listOf(
            AllowedField<RemoveBotFromFlatIn>("RemoveBotFromFlat.botId", AllowedRule.Lookup("bot", whole = false), true) { it.botId },
        ),
        "RemoveFriendIn" to listOf(
            AllowedField<RemoveFriendIn>("RemoveFriend.userIds", AllowedRule.Lookup("player", whole = false), true) { it.userIds },
        ),
        "RemoveItemFromTradeIn" to listOf(
            AllowedField<RemoveItemFromTradeIn>("RemoveItemFromTrade.itemId", AllowedRule.Lookup("ownFurni", whole = false), true) { it.itemId },
        ),
        "RemoveItemIn" to listOf(
            AllowedField<RemoveItemIn>("RemoveItem.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "RemoveOwnRoomRightsRoomIn" to listOf(
            AllowedField<RemoveOwnRoomRightsRoomIn>("RemoveOwnRoomRightsRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "RemovePetFromFlatIn" to listOf(
            AllowedField<RemovePetFromFlatIn>("RemovePetFromFlat.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "RemoveRightsIn" to listOf(
            AllowedField<RemoveRightsIn>("RemoveRights.userIds", AllowedRule.Lookup("player", whole = false), true) { it.userIds },
        ),
        "RemoveSaddleFromPetIn" to listOf(
            AllowedField<RemoveSaddleFromPetIn>("RemoveSaddleFromPet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "RentableSpaceCancelRentIn" to listOf(
            AllowedField<RentableSpaceCancelRentIn>("RentableSpaceCancelRent.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "RentableSpaceRentIn" to listOf(
            AllowedField<RentableSpaceRentIn>("RentableSpaceRent.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "RentableSpaceStatusIn" to listOf(
            AllowedField<RentableSpaceStatusIn>("RentableSpaceStatus.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "RequestRoomPropertySetIn" to listOf(
            AllowedField<RequestRoomPropertySetIn>("RequestRoomPropertySet.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ResetResolutionAchievementIn" to listOf(
            AllowedField<ResetResolutionAchievementIn>("ResetResolutionAchievement.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "RespectPetIn" to listOf(
            AllowedField<RespectPetIn>("RespectPet.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "RespectPlayerIn" to listOf(
            AllowedField<RespectPlayerIn>("RespectPlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "RoomAdSearchIn" to listOf(
            AllowedField<RoomAdSearchIn>("RoomAdSearch.searchType", AllowedRule.Values(setOf(16L, 17L)), false) { it.searchType },
        ),
        "RoomNetworkOpenConnectionIn" to listOf(
            AllowedField<RoomNetworkOpenConnectionIn>("RoomNetworkOpenConnection.homeRoomId", AllowedRule.Lookup("room", whole = false), true) { it.homeRoomId },
        ),
        "SaveRaidProtectionSettingsIn" to listOf(
            AllowedField<SaveRaidProtectionSettingsIn>("SaveRaidProtectionSettings.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "SaveRoomSettingsIn" to listOf(
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.doorMode", AllowedRule.Range(0L, 3L), false) { it.doorMode },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.maximumVisitors", AllowedRule.Range(10L, 75L), false) { it.maximumVisitors },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.categoryId", AllowedRule.Lookup("roomCategory", whole = false), false) { it.categoryId },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.tradeMode", AllowedRule.Range(0L, 2L), false) { it.tradeMode },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.wallThickness", AllowedRule.Range(-2L, 1L), false) { it.wallThickness },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.floorThickness", AllowedRule.Range(-2L, 1L), false) { it.floorThickness },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.whoCanMute", AllowedRule.Values(setOf(0L, 1L, 4L, 5L)), false) { it.whoCanMute },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.whoCanKick", AllowedRule.Values(setOf(0L, 1L, 2L, 4L, 5L)), false) { it.whoCanKick },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.whoCanBan", AllowedRule.Values(setOf(0L, 1L, 4L, 5L)), false) { it.whoCanBan },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.floodSensitivity", AllowedRule.Range(0L, 2L), false) { it.floodSensitivity },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.idleSleepTimeout", AllowedRule.Range(0L, 3600L), false) { it.idleSleepTimeout },
            AllowedField<SaveRoomSettingsIn>("SaveRoomSettings.idleAutokickTimeout", AllowedRule.Range(0L, 36000L), false) { it.idleAutokickTimeout },
        ),
        "SaveWardrobeOutfitIn" to listOf(
            AllowedField<SaveWardrobeOutfitIn>("SaveWardrobeOutfit.gender", AllowedRule.Values(setOf("M", "F", "m", "f")), false) { it.gender },
            AllowedField<SaveWardrobeOutfitIn>("SaveWardrobeOutfit.look", AllowedRule.Lookup("figure", whole = false), false) { it.look },
        ),
        "SelectFavouriteGroupIn" to listOf(
            AllowedField<SelectFavouriteGroupIn>("SelectFavouriteGroup.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "SendIconIn" to listOf(
            AllowedField<SendIconIn>("SendIcon.friendId", AllowedRule.Lookup("player", whole = false), true) { it.friendId },
        ),
        "SendMsgIn" to listOf(
            AllowedField<SendMsgIn>("SendMsg.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "SendRoomInviteIn" to listOf(
            AllowedField<SendRoomInviteIn>("SendRoomInvite.userIds", AllowedRule.Lookup("player", whole = false), true) { it.userIds },
        ),
        "SetActivatedBadgesIn" to listOf(
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.badge1", AllowedRule.Lookup("ownBadge", whole = false), true) { it.badge1 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.badge2", AllowedRule.Lookup("ownBadge", whole = false), true) { it.badge2 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.badge3", AllowedRule.Lookup("ownBadge", whole = false), true) { it.badge3 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.badge4", AllowedRule.Lookup("ownBadge", whole = false), true) { it.badge4 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.badge5", AllowedRule.Lookup("ownBadge", whole = false), true) { it.badge5 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.slot1", AllowedRule.Values(setOf(1L)), false) { it.slot1 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.slot2", AllowedRule.Values(setOf(2L)), false) { it.slot2 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.slot3", AllowedRule.Values(setOf(3L)), false) { it.slot3 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.slot4", AllowedRule.Values(setOf(4L)), false) { it.slot4 },
            AllowedField<SetActivatedBadgesIn>("SetActivatedBadges.slot5", AllowedRule.Values(setOf(5L)), false) { it.slot5 },
        ),
        "SetAdjacentCustomStackingHeightIn" to listOf(
            AllowedField<SetAdjacentCustomStackingHeightIn>("SetAdjacentCustomStackingHeight.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetAreaHideDataIn" to listOf(
            AllowedField<SetAreaHideDataIn>("SetAreaHideData.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetChatStylePreferenceIn" to listOf(
            AllowedField<SetChatStylePreferenceIn>("SetChatStylePreference.styleId", AllowedRule.Range(0L, 10999L), false) { it.styleId },
            AllowedField<SetChatStylePreferenceIn>("SetChatStylePreference.variant", AllowedRule.Range(0L, 4L), false) { it.variant },
        ),
        "SetChestNotificationSettingsIn" to listOf(
            AllowedField<SetChestNotificationSettingsIn>("SetChestNotificationSettings.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetChestOptionsIn" to listOf(
            AllowedField<SetChestOptionsIn>("SetChestOptions.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetChestPreferencesIn" to listOf(
            AllowedField<SetChestPreferencesIn>("SetChestPreferences.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetClothingChangeDataIn" to listOf(
            AllowedField<SetClothingChangeDataIn>("SetClothingChangeData.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<SetClothingChangeDataIn>("SetClothingChangeData.gender", AllowedRule.Values(setOf("M", "F", "m", "f")), false) { it.gender },
        ),
        "SetFurnitureLabelIn" to listOf(
            AllowedField<SetFurnitureLabelIn>("SetFurnitureLabel.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "SetFurnitureLabelSettingsIn" to listOf(
            AllowedField<SetFurnitureLabelSettingsIn>("SetFurnitureLabelSettings.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "SetItemDataIn" to listOf(
            AllowedField<SetItemDataIn>("SetItemData.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
            AllowedField<SetItemDataIn>("SetItemData.color", AllowedRule.Values(setOf("9CCEFF", "FF9CFF", "9CFF9C", "FFFF33", "FFFFFF", "FF9C9C", "FFCC66", "9CFFFF")), false) { it.color },
        ),
        "SetMannequinFigureIn" to listOf(
            AllowedField<SetMannequinFigureIn>("SetMannequinFigure.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetMannequinNameIn" to listOf(
            AllowedField<SetMannequinNameIn>("SetMannequinName.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetNewNavigatorWindowPreferencesIn" to listOf(
            AllowedField<SetNewNavigatorWindowPreferencesIn>("SetNewNavigatorWindowPreferences.resultsMode", AllowedRule.Values(setOf(0L)), false) { it.resultsMode },
        ),
        "SetObjectDataIn" to listOf(
            AllowedField<SetObjectDataIn>("SetObjectData.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetRandomStateIn" to listOf(
            AllowedField<SetRandomStateIn>("SetRandomState.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SetRelationshipStatusIn" to listOf(
            AllowedField<SetRelationshipStatusIn>("SetRelationshipStatus.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<SetRelationshipStatusIn>("SetRelationshipStatus.relationId", AllowedRule.Range(0L, 3L), false) { it.relationId },
        ),
        "SetSoundSettingsIn" to listOf(
            AllowedField<SetSoundSettingsIn>("SetSoundSettings.system", AllowedRule.Range(0L, 100L), false) { it.system },
            AllowedField<SetSoundSettingsIn>("SetSoundSettings.furni", AllowedRule.Range(0L, 100L), false) { it.furni },
            AllowedField<SetSoundSettingsIn>("SetSoundSettings.trax", AllowedRule.Range(0L, 100L), false) { it.trax },
        ),
        "SetTargetedOfferStateIn" to listOf(
            AllowedField<SetTargetedOfferStateIn>("SetTargetedOfferState.state", AllowedRule.Values(setOf(1L, 4L)), false) { it.state },
        ),
        "SetYoutubeDisplayPlaylistIn" to listOf(
            AllowedField<SetYoutubeDisplayPlaylistIn>("SetYoutubeDisplayPlaylist.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "ShoutIn" to listOf(
            AllowedField<ShoutIn>("Shout.int1", AllowedRule.Range(0L, 10999L), false) { it.int1 },
        ),
        "SignIn" to listOf(
            AllowedField<SignIn>("Sign.signId", AllowedRule.Range(0L, 17L), false) { it.signId },
        ),
        "SpinWheelOfFortuneIn" to listOf(
            AllowedField<SpinWheelOfFortuneIn>("SpinWheelOfFortune.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "SubmitRoomToCompetitionIn" to listOf(
            AllowedField<SubmitRoomToCompetitionIn>("SubmitRoomToCompetition.level", AllowedRule.Range(0L, 3L), false) { it.level },
        ),
        "SynchronizeVariableAddonIn" to listOf(
            AllowedField<SynchronizeVariableAddonIn>("SynchronizeVariableAddon.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "ThrowDiceIn" to listOf(
            AllowedField<ThrowDiceIn>("ThrowDice.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "TogglePetBreedingPermissionIn" to listOf(
            AllowedField<TogglePetBreedingPermissionIn>("TogglePetBreedingPermission.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "TogglePetRidingPermissionIn" to listOf(
            AllowedField<TogglePetRidingPermissionIn>("TogglePetRidingPermission.petId", AllowedRule.Lookup("pet", whole = false), true) { it.petId },
        ),
        "UnbanPlayerFromRoomIn" to listOf(
            AllowedField<UnbanPlayerFromRoomIn>("UnbanPlayerFromRoom.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<UnbanPlayerFromRoomIn>("UnbanPlayerFromRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "UnblockGroupMemberIn" to listOf(
            AllowedField<UnblockGroupMemberIn>("UnblockGroupMember.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<UnblockGroupMemberIn>("UnblockGroupMember.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "UnblockPlayerIn" to listOf(
            AllowedField<UnblockPlayerIn>("UnblockPlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
        ),
        "UnmutePlayerIn" to listOf(
            AllowedField<UnmutePlayerIn>("UnmutePlayer.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<UnmutePlayerIn>("UnmutePlayer.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "UpdateActionIn" to listOf(
            AllowedField<UpdateActionIn>("UpdateAction.delay", AllowedRule.Lookup("wiredDelay", whole = false), false) { it.delay },
        ),
        "UpdateFigureDataIn" to listOf(
            AllowedField<UpdateFigureDataIn>("UpdateFigureData.genderCode", AllowedRule.Values(setOf("M", "F", "m", "f")), false) { it.genderCode },
            AllowedField<UpdateFigureDataIn>("UpdateFigureData.look", AllowedRule.Lookup("figure", whole = false), false) { it.look },
        ),
        "UpdateForumSettingsIn" to listOf(
            AllowedField<UpdateForumSettingsIn>("UpdateForumSettings.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<UpdateForumSettingsIn>("UpdateForumSettings.canRead", AllowedRule.Range(0L, 3L), false) { it.canRead },
            AllowedField<UpdateForumSettingsIn>("UpdateForumSettings.postMessages", AllowedRule.Range(0L, 3L), false) { it.postMessages },
            AllowedField<UpdateForumSettingsIn>("UpdateForumSettings.postThreads", AllowedRule.Range(0L, 3L), false) { it.postThreads },
            AllowedField<UpdateForumSettingsIn>("UpdateForumSettings.modForum", AllowedRule.Range(0L, 3L), false) { it.modForum },
        ),
        "UpdateGuildBadgeIn" to listOf(
            AllowedField<UpdateGuildBadgeIn>("UpdateGuildBadge.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
            AllowedField<UpdateGuildBadgeIn>("UpdateGuildBadge.parts", AllowedRule.Lookup("guildBadge", whole = true), false) { it.parts },
        ),
        "UpdateGuildColorsIn" to listOf(
            AllowedField<UpdateGuildColorsIn>("UpdateGuildColors.int2", AllowedRule.Lookup("guildSymbolColour", whole = false), false) { it.int2 },
            AllowedField<UpdateGuildColorsIn>("UpdateGuildColors.int3", AllowedRule.Lookup("guildBackgroundColour", whole = false), false) { it.int3 },
            AllowedField<UpdateGuildColorsIn>("UpdateGuildColors.int1", AllowedRule.Lookup("guild", whole = false), true) { it.int1 },
        ),
        "UpdateGuildIdentityIn" to listOf(
            AllowedField<UpdateGuildIdentityIn>("UpdateGuildIdentity.int1", AllowedRule.Lookup("guild", whole = false), true) { it.int1 },
        ),
        "UpdateGuildSettingsIn" to listOf(
            AllowedField<UpdateGuildSettingsIn>("UpdateGuildSettings.int2", AllowedRule.Range(0L, 4L), false) { it.int2 },
            AllowedField<UpdateGuildSettingsIn>("UpdateGuildSettings.int3", AllowedRule.Range(0L, 1L), false) { it.int3 },
            AllowedField<UpdateGuildSettingsIn>("UpdateGuildSettings.int1", AllowedRule.Lookup("guild", whole = false), true) { it.int1 },
        ),
        "UpdateHomeRoomIn" to listOf(
            AllowedField<UpdateHomeRoomIn>("UpdateHomeRoom.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "UpdateRoomCategoryAndTradeSettingsIn" to listOf(
            AllowedField<UpdateRoomCategoryAndTradeSettingsIn>("UpdateRoomCategoryAndTradeSettings.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
            AllowedField<UpdateRoomCategoryAndTradeSettingsIn>("UpdateRoomCategoryAndTradeSettings.categoryId", AllowedRule.Lookup("roomCategory", whole = false), false) { it.categoryId },
            AllowedField<UpdateRoomCategoryAndTradeSettingsIn>("UpdateRoomCategoryAndTradeSettings.tradeMode", AllowedRule.Range(0L, 2L), false) { it.tradeMode },
        ),
        "UpdateRoomFilterIn" to listOf(
            AllowedField<UpdateRoomFilterIn>("UpdateRoomFilter.roomId", AllowedRule.Lookup("room", whole = false), true) { it.roomId },
        ),
        "UpdateThreadIn" to listOf(
            AllowedField<UpdateThreadIn>("UpdateThread.guildId", AllowedRule.Lookup("guild", whole = false), true) { it.guildId },
        ),
        "UpgradeChestIn" to listOf(
            AllowedField<UpgradeChestIn>("UpgradeChest.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "UseFurnitureIn" to listOf(
            AllowedField<UseFurnitureIn>("UseFurniture.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "UseWallItemIn" to listOf(
            AllowedField<UseWallItemIn>("UseWallItem.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "WhisperIn" to listOf(
            AllowedField<WhisperIn>("Whisper.styleId", AllowedRule.Range(0L, 10999L), false) { it.styleId },
        ),
        "WiredGenerateWebApiKeyIn" to listOf(
            AllowedField<WiredGenerateWebApiKeyIn>("WiredGenerateWebApiKey.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
        "WiredGetPlayerPermanentVariablesIn" to listOf(
            AllowedField<WiredGetPlayerPermanentVariablesIn>("WiredGetPlayerPermanentVariables.entityType", AllowedRule.Values(setOf(1L, 2L)), false) { it.entityType },
        ),
        "WiredLinkCreateNetworkIn" to listOf(
            AllowedField<WiredLinkCreateNetworkIn>("WiredLinkCreateNetwork.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredLinkDeleteNetworkIn" to listOf(
            AllowedField<WiredLinkDeleteNetworkIn>("WiredLinkDeleteNetwork.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredLinkGetConnectionsIn" to listOf(
            AllowedField<WiredLinkGetConnectionsIn>("WiredLinkGetConnections.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredLinkOpenIn" to listOf(
            AllowedField<WiredLinkOpenIn>("WiredLinkOpen.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredLinkRenameNetworkIn" to listOf(
            AllowedField<WiredLinkRenameNetworkIn>("WiredLinkRenameNetwork.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredLinkSaveConfigurationIn" to listOf(
            AllowedField<WiredLinkSaveConfigurationIn>("WiredLinkSaveConfiguration.furniId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.furniId },
        ),
        "WiredSetObjectVariableValueIn" to listOf(
            AllowedField<WiredSetObjectVariableValueIn>("WiredSetObjectVariableValue.action", AllowedRule.Range(0L, 2L), false) { it.action },
        ),
        "WiredSetPlayerPermanentVariableIn" to listOf(
            AllowedField<WiredSetPlayerPermanentVariableIn>("WiredSetPlayerPermanentVariable.userId", AllowedRule.Lookup("player", whole = false), true) { it.userId },
            AllowedField<WiredSetPlayerPermanentVariableIn>("WiredSetPlayerPermanentVariable.action", AllowedRule.Range(0L, 2L), false) { it.action },
            AllowedField<WiredSetPlayerPermanentVariableIn>("WiredSetPlayerPermanentVariable.entityType", AllowedRule.Values(setOf(1L, 2L)), false) { it.entityType },
        ),
        "WithdrawAllFromChestIn" to listOf(
            AllowedField<WithdrawAllFromChestIn>("WithdrawAllFromChest.itemId", AllowedRule.Lookup("furniInRoom", whole = false), true) { it.itemId },
        ),
    )

    /** The rows of the message called [name]; a codec passes its own type as [T]. */
    @Suppress("UNCHECKED_CAST")
    fun <T> of(name: String): List<AllowedField<T>> = (rows[name] ?: emptyList()) as List<AllowedField<T>>

    /** The lookup names the table uses. */
    val lookups: Set<String> = rows.values.flatten().mapNotNull { (it.rule as? AllowedRule.Lookup)?.name }.toSet()
}
