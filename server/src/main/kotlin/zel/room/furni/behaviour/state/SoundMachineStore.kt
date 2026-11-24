package zel.room.furni.behaviour.state

import zel.db.Sql
import javax.sql.DataSource

fun interface SoundMachineStore {
    fun songs(furniId: Int): List<Int>

    companion object {
        fun jdbc(dataSource: DataSource): SoundMachineStore {
            val sql = Sql(dataSource)
            return SoundMachineStore { furniId -> sql.select(SONGS, furniId).rows { it.getInt("soundtrack_id") } }
        }

        private const val SONGS = "SELECT soundtrack_id FROM sound_machine_songs WHERE furni_id = ? ORDER BY slot"
    }
}
