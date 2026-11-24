package zel.protocol.rewardtracks

import zel.net.clients.ClientConnection
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.RewardTrackClaimResultOut
import zel.protocol.generated.RewardTrackClaimResultOutCodec
import zel.protocol.generated.RewardTrackPremiumPurchaseResultOut
import zel.protocol.generated.RewardTrackPremiumPurchaseResultOutCodec
import zel.protocol.generated.RewardTrackProgressOut
import zel.protocol.generated.RewardTrackProgressOutCodec
import zel.protocol.generated.RewardTracksOut
import zel.protocol.generated.RewardTracksOutCodec

class RewardTrackMessages {
    fun tracks(
        client: ClientConnection,
        disabled: Boolean,
        entries: List<RewardTrackEntry>,
        reload: Boolean,
    ) = client.send(RewardTracksOutCodec, tracksMessage(disabled, entries, reload))

    fun progress(
        client: ClientConnection,
        trackId: String,
        taskId: String,
        progressCount: Int,
        points: Int,
    ) = client.send(
        RewardTrackProgressOutCodec,
        RewardTrackProgressOut(trackId, taskId, progressCount, points),
    )

    fun claimResult(
        client: ClientConnection,
        trackId: String,
        prizeId: String,
        result: Int,
    ) = client.send(
        RewardTrackClaimResultOutCodec,
        RewardTrackClaimResultOut(trackId, prizeId, result),
    )

    fun passResult(
        client: ClientConnection,
        trackId: String,
        result: Int,
        points: Int,
    ) = client.send(
        RewardTrackPremiumPurchaseResultOutCodec,
        RewardTrackPremiumPurchaseResultOut(trackId, result, points),
    )

    companion object {
        fun tracksMessage(
            disabled: Boolean,
            entries: List<RewardTrackEntry>,
            reload: Boolean,
        ): RewardTracksOut {
            val rest = PacketWriter()
            rest.countInt(entries.size)
            entries.forEach { writeTrack(rest, it) }
            rest.boolean(reload)
            return RewardTracksOut(disabled, rest.toByteArray())
        }

        private fun writeTrack(
            w: PacketWriter,
            entry: RewardTrackEntry,
        ) {
            val track = entry.track
            val freeClaimed = track.prizes.filter { !it.premium }.all { it.id in entry.claimed }
            val premiumClaimed = track.prizes.filter { it.premium }.all { it.id in entry.claimed }
            val offer = entry.offer
            w.string(track.id)
            w.string(track.theme)
            w.int(entry.points)
            w.boolean(offer != null)
            if (offer != null) {
                w.double(offer.pointsFactor)
                w.int(offer.instantPoints)
                w.int(offer.premium)
                w.int(offer.credits)
            }
            w.boolean(entry.premium)
            w.boolean(freeClaimed)
            w.boolean(offer == null || (entry.premium && premiumClaimed))
            w.countInt(track.tasks.size)
            for (task in track.tasks) {
                w.string(task.id)
                w.string(task.actionType)
                w.string(task.parameter)
                w.int(entry.progress[task.id] ?: 0)
                w.boolean(task.premium)
                w.countInt(task.levels.size)
                for (level in task.levels) {
                    w.int(level.requiredCount)
                    w.int(level.points)
                    w.boolean(level.premium)
                }
            }
            w.countInt(track.prizes.size)
            for (prize in track.prizes) {
                w.string(prize.id)
                w.int(prize.requiredPoints)
                w.short(prize.productType.toShort())
                w.string(prize.productCode)
                w.string(prize.extra)
                w.int(prize.amount)
                w.boolean(prize.premium)
                w.boolean((!prize.premium || entry.premium) && entry.points >= prize.requiredPoints)
                w.boolean(prize.id in entry.claimed)
            }
        }
    }
}
