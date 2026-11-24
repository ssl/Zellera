package zel.protocol.outgoing.nux

import zel.game.items.WelcomeGift
import zel.protocol.generated.NewPlayerExperienceGiftOfferOut
import zel.protocol.generated.NewPlayerExperienceGiftOfferOutCodec
import zel.protocol.outgoing.OutgoingMessage

class NewUserGiftMessage(
    private val options: List<List<WelcomeGift>>,
) : OutgoingMessage<NewPlayerExperienceGiftOfferOut>(NewPlayerExperienceGiftOfferOutCodec) {
    override fun message() =
        NewPlayerExperienceGiftOfferOut(
            options.map { option ->
                NewPlayerExperienceGiftOfferOut.List1Item(
                    OFFER_DAY,
                    OFFER_STEP,
                    option.map { gift ->
                        NewPlayerExperienceGiftOfferOut.List1Item.List1Item(
                            gift.image.orEmpty(),
                            listOf(
                                NewPlayerExperienceGiftOfferOut.List1Item.List1Item.List1Item(
                                    gift.furniName.orEmpty(),
                                    gift.roomCode.orEmpty(),
                                ),
                            ),
                        )
                    },
                )
            },
        )

    private companion object {
        const val OFFER_DAY = 1
        const val OFFER_STEP = 3
    }
}
