package app.auriel.basalt.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Repeat days as a 7-bit mask; see core.time.Weekdays. */
    val repeatBits: Int,
    val label: String,
    val enabled: Boolean,
    val vibrate: Boolean,
    val ringtoneUri: String?,
    val ringtoneStartMillis: Long = 0L,
    val ringtoneEndMillis: Long = 0L,
    val skipNext: Boolean,
)

/**
 * One scheduled firing.
 *
 * The trigger is stored as *local date and time*, not as an instant. An
 * alarm set for 07:00 means 07:00 wherever the user wakes up; storing an
 * instant would make it ring at 07:00 in the zone it was created in and at
 * the wrong hour everywhere else.
 */
@Entity(
    tableName = "alarm_instances",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("alarmId"), Index("firesAtEpochMillis")],
)
data class AlarmInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    /**
     * The same moment resolved against the zone in force when it was
     * scheduled. Denormalised so the scheduler can sort and query without
     * re-resolving every row, and recomputed whenever the zone changes.
     */
    val firesAtEpochMillis: Long,
    val state: String,
)

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalMillis: Long,
    val label: String,
    val state: String,
    /**
     * Deadline on the *wall* clock, not the monotonic one.
     *
     * `SystemClock.elapsedRealtime()` restarts at zero on reboot, so a
     * monotonic deadline is meaningless once persisted. The wall-clock
     * deadline survives; the monotonic one is derived from it at load.
     */
    val deadlineWallMillis: Long,
    val pausedRemainingMillis: Long,
    val createdAtMillis: Long,
)

@Entity(tableName = "laps")
data class LapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Int,
    val lapMillis: Long,
    val totalMillis: Long,
)
