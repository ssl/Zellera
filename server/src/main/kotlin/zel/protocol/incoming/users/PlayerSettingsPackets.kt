package zel.protocol.incoming.users

import zel.core.rules.rules
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.permissions.Privilege
import zel.game.players.ChatStyles
import zel.game.players.Player
import zel.game.players.PlayerSettings
import zel.game.players.subscriptions.ClubPayday
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnection
import zel.protocol.generated.AddToCustomFilterInCodec
import zel.protocol.generated.BlockListInitInCodec
import zel.protocol.generated.BlockPlayerInCodec
import zel.protocol.generated.GetCreditsInfoInCodec
import zel.protocol.generated.GetCustomFilterInCodec
import zel.protocol.generated.GetCustomFilterResultOut
import zel.protocol.generated.GetCustomFilterResultOutCodec
import zel.protocol.generated.GetIgnoredPlayersInCodec
import zel.protocol.generated.GetPlayerNftChatStylesInCodec
import zel.protocol.generated.GetSoundSettingsInCodec
import zel.protocol.generated.IgnoredPlayersOut
import zel.protocol.generated.IgnoredPlayersOutCodec
import zel.protocol.generated.InfoRetrieveInCodec
import zel.protocol.generated.NewPlayerExperienceGetGiftsInCodec
import zel.protocol.generated.NewPlayerExperienceScriptProceedInCodec
import zel.protocol.generated.RemoveFromCustomFilterInCodec
import zel.protocol.generated.ReplenishRespectInCodec
import zel.protocol.generated.ScrGetKickbackInfoInCodec
import zel.protocol.generated.ScrGetPlayerInfoInCodec
import zel.protocol.generated.ScrSendKickbackInfoOutCodec
import zel.protocol.generated.SetChatPreferencesInCodec
import zel.protocol.generated.SetChatStylePreferenceInCodec
import zel.protocol.generated.SetIgnoreRoomInvitesInCodec
import zel.protocol.generated.SetOnlineIndicatorPreferenceInCodec
import zel.protocol.generated.SetRoomCameraPreferencesInCodec
import zel.protocol.generated.SetSoundSettingsInCodec
import zel.protocol.generated.SetUIFlagsInCodec
import zel.protocol.generated.UnblockPlayerInCodec
import zel.protocol.generated.WiredSetPreferencesInCodec
import zel.protocol.outgoing.users.BlockList
import zel.protocol.outgoing.users.BlockResult
import zel.protocol.outgoing.users.CustomWordFilterModifyResult
import zel.protocol.outgoing.users.MeMenuSettings
import zel.protocol.outgoing.users.NftChatStyles
import zel.protocol.outgoing.users.PurchasableChatStyles
import zel.protocol.outgoing.users.UserClub
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.outgoing.users.UserCurrency
import zel.protocol.outgoing.users.UserData
import zel.protocol.outgoing.users.UserPerks
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.player
import zel.server.GameServices

class PlayerSettingsPackets(
    private val services: GameServices,
    tour: NewUserTour,
) : PacketPackage {
    override val name = "settings"

    override val routes =
        listOf(
            PacketRoute(InfoRetrieveInCodec) { s, _ -> sendAccount(s.client, s.player) },
            PacketRoute(GetCreditsInfoInCodec) { s, _ ->
                s.client.send(UserCredits(s.player))
                s.client.send(UserCurrency(s.player))
            },
            PacketRoute(ScrGetPlayerInfoInCodec) { s, _ ->
                s.client.send(UserClub(s.player, UserClub.RESPONSE_REQUESTED))
            },
            PacketRoute(ScrGetKickbackInfoInCodec) { s, _ ->
                s.send(ScrSendKickbackInfoOutCodec, ClubPayday.forecast(services, s.player.profile).message())
            },
            PacketRoute(GetSoundSettingsInCodec) { s, _ -> s.client.send(MeMenuSettings(s.player)) },
            PacketRoute(SetSoundSettingsInCodec) { s, m ->
                s.player.store {
                    volumes = PlayerSettings.Volumes(volume(m.system), volume(m.furni), volume(m.trax))
                }
            },
            PacketRoute(SetUIFlagsInCodec) { s, m -> s.player.store { clientFlags = uiFlags(m.flags) } },
            PacketRoute(SetChatPreferencesInCodec) { s, m ->
                s.player.stats.settings.update(
                    PlayerSettings.ChatLayout(
                        PlayerSettingRules.chatMode(m.chatMode),
                        PlayerSettingRules.bubbleWidth(m.bubbleWidth),
                        PlayerSettingRules.scrollSpeed(m.scrollSpeed),
                    ),
                )
            },
            PacketRoute(SetChatStylePreferenceInCodec) { s, m -> changeChatBubble(s.player, m.styleId) },
            PacketRoute(SetIgnoreRoomInvitesInCodec) { s, m -> s.player.store { noRoomInvites = m.ignore } },
            PacketRoute(SetRoomCameraPreferencesInCodec) { s, m ->
                s.player.store { noCameraFollow = m.blockFollow }
            },
            PacketRoute(SetOnlineIndicatorPreferenceInCodec) { s, m ->
                s.player.stats.settings
                    .updateOnlineIndicator(PlayerSettingRules.onlineIndicator(m.preference))
            },
            PacketRoute(WiredSetPreferencesInCodec) { s, m ->
                val settings = s.player.stats.settings
                settings.update(PlayerSettings.WiredMenu(m.menuButton, m.inspectButton, m.playtest))
                settings.updateWiredWhisper(m.wiredWhisperDisabled)
            },
            PacketRoute(GetPlayerNftChatStylesInCodec) { s, _ ->
                s.client.send(NftChatStyles(services.chatStyles.nftStylesFor(s.player)))
            },
            PacketRoute(GetIgnoredPlayersInCodec) { s, _ ->
                s.client.send(IgnoredPlayersOutCodec, IgnoredPlayersOut(s.player.stats.contacts.ignoredIds))
            },
            PacketRoute(BlockListInitInCodec) { s, _ ->
                s.client.send(BlockList(s.player.stats.contacts.blockedIds))
            },
            PacketRoute(BlockPlayerInCodec) { s, m ->
                if (s.player.stats.contacts
                        .block(m.userId)
                ) {
                    s.client.send(BlockResult(BlockResult.BLOCKED, m.userId))
                }
            },
            PacketRoute(UnblockPlayerInCodec) { s, m ->
                if (s.player.stats.contacts
                        .unblock(m.userId)
                ) {
                    s.client.send(BlockResult(BlockResult.UNBLOCKED, m.userId))
                }
            },
            PacketRoute(GetCustomFilterInCodec) { s, _ ->
                s.client.send(
                    GetCustomFilterResultOutCodec,
                    GetCustomFilterResultOut(
                        s.player.stats.contacts.customFilter
                            .words(),
                    ),
                )
            },
            PacketRoute(AddToCustomFilterInCodec) { s, m -> changeFilterWord(s.client, s.player, m.word, add = true) },
            PacketRoute(RemoveFromCustomFilterInCodec) { s, m ->
                changeFilterWord(s.client, s.player, m.word, add = false)
            },
            PacketRoute(ReplenishRespectInCodec) { s, _ -> replenishRespect(s.client, s.player) },
            PacketRoute(NewPlayerExperienceScriptProceedInCodec) { s, _ -> tour.proceed(s.player) },
            PacketRoute(NewPlayerExperienceGetGiftsInCodec) { s, m ->
                m.gifts.firstOrNull()?.let { tour.pickGift(s.client, it.index) }
            },
        )

    private inline fun Player.store(change: PlayerSettings.() -> Unit) {
        stats.settings.change()
        stats.settings.savePreferences()
    }

    private fun sendAccount(
        client: ClientConnection,
        player: Player,
    ) {
        client.sendAll(
            listOf(
                UserData(player).compose(),
                UserPerks(player).compose(),
                PurchasableChatStyles(shopStyles(player)).compose(),
            ),
        )
    }

    private fun shopStyles(player: Player): List<Int> =
        if (player.hasPermission(Privilege.CHAT_ANY_BUBBLE)) {
            ChatStyles.SHOP_STYLES.toList()
        } else {
            services.chatStyles.boughtStyles(player)
        }

    private fun changeChatBubble(
        player: Player,
        styleId: Int,
    ) {
        if (!services.chatStyles.allows(player, styleId)) return
        player.store { bubble = services.chatBubbles.of(styleId) }
    }

    private fun changeFilterWord(
        client: ClientConnection,
        player: Player,
        word: String,
        add: Boolean,
    ) {
        val contacts = player.stats.contacts
        val result =
            when {
                !WORD.matches(word.trim()) -> CustomWordFilterModifyResult.FAILED
                add -> if (contacts.addFilterWord(word)) CustomWordFilterModifyResult.ADDED else FAILED
                else -> if (contacts.removeFilterWord(word)) CustomWordFilterModifyResult.REMOVED else FAILED
            }
        client.send(CustomWordFilterModifyResult(result, word))
    }

    private fun replenishRespect(
        client: ClientConnection,
        player: Player,
    ) {
        val tally = player.stats.tally
        val price = rules.respectRefillCost
        synchronized(tally) {
            if (tally.respectRefillsLeft <= 0) return
            if (price > 0 && !payRefill(player, price)) return
            tally.refillRespects(rules.dailyRespects)
        }
        PlayerActions.progress(player, CountedAction.REPLENISH_RESPECT, 1)
        client.send(UserData(player))
    }

    private fun payRefill(
        player: Player,
        price: Int,
    ): Boolean {
        val operation = LedgerEntry.newKey("respect-refill:${player.profile.id}")
        return player.wallet.takePoints(EconomyLedger.POINTS, price, REFILL_REASON, operation, null)
    }

    companion object {
        private const val FAILED = CustomWordFilterModifyResult.FAILED
        private const val MAX_VOLUME = 100
        private const val FLAG_BITS = 0xFFFF

        private const val REFILL_REASON = "respect.refill"

        private val WORD = Regex("[\\p{L}\\p{N}]{1,${zel.game.players.CustomFilter.LONGEST_WORD}}")

        fun volume(sent: Int): Int = sent.coerceIn(0, MAX_VOLUME)

        fun uiFlags(sent: Int): Int = if (sent < 0) 0 else sent and FLAG_BITS
    }
}
