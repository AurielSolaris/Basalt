package app.auriel.basalt.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlarmEntity::class,
        AlarmInstanceEntity::class,
        TimerEntity::class,
        LapEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BasaltDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun alarmInstances(): AlarmInstanceDao
    abstract fun timers(): TimerDao
    abstract fun laps(): LapDao

    companion object {
        /**
         * `exportSchema = false` while the schema is still moving. Before
         * the first release that anyone else installs, this flips to true
         * and the schema JSON is committed — migrations are unwritable
         * without it, and a clock app that loses the user's alarms on
         * upgrade has failed at its only job.
         */
        fun create(context: Context): BasaltDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BasaltDatabase::class.java,
                "basalt.db",
            )
                // Pre-1.0 only. Nothing has shipped, so a schema change that
                // drops development data is an acceptable trade for not
                // hand-writing migrations against a moving target. This comes
                // out — with exportSchema = true and real migrations — before
                // the first release anyone else installs.
                .fallbackToDestructiveMigration()
                .build()
    }
}
