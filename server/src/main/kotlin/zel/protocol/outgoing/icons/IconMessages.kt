package zel.protocol.outgoing.icons

import zel.game.icons.IconRoster
import zel.protocol.generated.IconInfoOut
import zel.protocol.generated.IconShopDataOut
import zel.protocol.generated.PlayerIconsOut

fun iconInfo(item: IconRoster.Icon): IconInfoOut =
    with(item.price) {
        IconInfoOut(item.id, item.name, item.setId, item.state.code, credits, points, pointsType)
    }

fun playerIcons(snapshot: IconRoster): PlayerIconsOut =
    PlayerIconsOut(
        snapshot
            .icons
            .values
            .filter { it.state.countsForSet }
            .map { PlayerIconsOut.List1Item(it.id, it.state.code) },
        snapshot.lastUsed,
    )

fun iconShopData(snapshot: IconRoster): IconShopDataOut =
    IconShopDataOut(
        snapshot.sets.map { set ->
            IconShopDataOut.List1Item(
                set.id,
                set.name,
                set.completed,
                set.rewardId,
                set.rewardState.code,
                set.price.credits,
                set.price.points,
                set.price.pointsType,
                set.icons.map { item ->
                    IconShopDataOut.List1Item.List1Item(
                        item.id,
                        item.name,
                        item.setId,
                        item.state.code,
                        item.price.credits,
                        item.price.points,
                        item.price.pointsType,
                    )
                },
            )
        },
    )
