package zel.protocol.communitygoals

import zel.game.communitygoals.ConcurrentPlayersGoal
import zel.game.communitygoals.TieredCommunityGoal
import zel.protocol.generated.CommunityGoalProgressOut
import zel.protocol.generated.CommunityGoalProgressOutCodec
import zel.protocol.generated.CommunityGoalVoteInCodec
import zel.protocol.generated.CommunityVoteReceivedOut
import zel.protocol.generated.CommunityVoteReceivedOutCodec
import zel.protocol.generated.ConcurrentPlayersGoalProgressOut
import zel.protocol.generated.ConcurrentPlayersGoalProgressOutCodec
import zel.protocol.generated.GetCommunityGoalProgressInCodec
import zel.protocol.generated.GetConcurrentPlayersGoalProgressInCodec
import zel.protocol.generated.GetConcurrentPlayersRewardInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession

class CommunityGoalPackets(
    goal: TieredCommunityGoal,
    private val concurrent: ConcurrentPlayersGoal,
) : PacketPackage {
    override val name = "communitygoals"

    private val progressCooldown = ClientCooldown("communitygoals.progress", PROGRESS_COOLDOWN_MS)
    private val voteCooldown = ClientCooldown("communitygoals.vote", COOLDOWN_MS)
    private val concurrentCooldown = ClientCooldown("communitygoals.concurrent", COOLDOWN_MS)
    private val rewardCooldown = ClientCooldown("communitygoals.reward", COOLDOWN_MS)

    override val routes =
        listOf(
            PacketRoute(GetCommunityGoalProgressInCodec) { session, _ ->
                if (progressCooldown.acquire(session.client)) {
                    val progress = goal.progressFor(requireNotNull(session.client.player).profile.id)
                    session.send(
                        CommunityGoalProgressOutCodec,
                        CommunityGoalProgressOut(
                            progress.isOver,
                            progress.personalScore,
                            progress.personalRank,
                            progress.totalScore,
                            progress.reach.level,
                            progress.reach.missing,
                            progress.reach.percent,
                            progress.code,
                            progress.secondsLeft,
                            progress.rewardLimits,
                        ),
                    )
                }
            },
            PacketRoute(CommunityGoalVoteInCodec) { session, m ->
                if (voteCooldown.acquire(session.client)) {
                    val accepted = goal.vote(requireNotNull(session.client.player).profile.id, m.optionId)
                    session.send(CommunityVoteReceivedOutCodec, CommunityVoteReceivedOut(accepted))
                }
            },
            PacketRoute(GetConcurrentPlayersGoalProgressInCodec) { session, _ ->
                if (concurrentCooldown.acquire(session.client)) {
                    sendConcurrentUsers(session)
                }
            },
            PacketRoute(GetConcurrentPlayersRewardInCodec) { session, _ ->
                if (rewardCooldown.acquire(session.client)) {
                    concurrent.claimReward(requireNotNull(session.client.player))
                    sendConcurrentUsers(session)
                }
            },
        )

    private fun sendConcurrentUsers(session: PacketSession) {
        session.send(
            ConcurrentPlayersGoalProgressOutCodec,
            ConcurrentPlayersGoalProgressOut(
                concurrent.stateFor(requireNotNull(session.client.player).profile.id).code,
                concurrent.online,
                concurrent.target,
            ),
        )
    }

    private companion object {
        const val PROGRESS_COOLDOWN_MS = 500L
        const val COOLDOWN_MS = 1000L
    }
}
