package app.auriel.basalt.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        AlarmEntity::class,
        AlarmInstanceEntity::class,
        TimerEntity::class,
        LapEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BasaltDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun alarmInstances(): AlarmInstanceDao
    abstract fun timers(): TimerDao
    abstract fun laps(): LapDao

    companion object {

        /**
         * Every schema change from here on needs one of these.
         *
         * The list is empty and that is not an oversight: version 2 is the
         * first schema that ever reached a device. Version 1 existed only
         * inside a single development branch, so there is no installed base
         * to migrate from — but there is one now, which is why
         * `fallbackToDestructiveMigration()` is gone.
         *
         * Without a fallback, a schema change with no matching migration
         * throws on open instead of silently deleting the user's alarms.
         * That is the correct trade: a crash is a bug report, and a wiped
         * alarm list is a missed morning nobody can explain.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): BasaltDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BasaltDatabase::class.java,
                "basalt.db",
            )
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
