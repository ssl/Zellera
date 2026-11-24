package zel.room.games

class GameTeam(
    val color: TeamColor,
) {
    private val players = mutableListOf<TeamMember>()

    val members: List<TeamMember> get() = players

    /** The points the team earned itself, never so low that its total goes below 0. */
    var teamScore: Int = 0
        private set

    val totalScore: Int get() = teamScore + players.sumOf { it.score }

    fun join(member: TeamMember) {
        players += member
    }

    fun leave(member: TeamMember) {
        players -= member
    }

    fun addTeamScore(points: Int) {
        teamScore = maxOf(teamScore + points, -players.sumOf { it.score })
    }

    fun resetScores() {
        players.forEach(TeamMember::reset)
        teamScore = 0
    }
}
