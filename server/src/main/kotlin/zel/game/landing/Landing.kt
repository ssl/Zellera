package zel.game.landing

import zel.server.GameServices

class Landing(
    services: GameServices,
) {
    val hallOfFame = HallOfFameBoard(services.database)
    val news = NewsArticles(services.database, services.config)
}
