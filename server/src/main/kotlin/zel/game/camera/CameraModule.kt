package zel.game.camera

import zel.core.Configuration
import zel.db.Sql
import javax.sql.DataSource

class CameraModule(
    config: Configuration,
    dataSource: DataSource,
    now: () -> Int,
) {
    val service = CameraService(config, dataSource)
    val competition = PhotoCompetition(config, Sql(dataSource), now)
}
