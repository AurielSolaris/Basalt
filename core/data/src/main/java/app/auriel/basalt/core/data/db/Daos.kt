package app.auriel.basalt.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): AlarmEntity?

    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET skipNext = :skip WHERE id = :id")
    suspend fun setSkipNext(id: Long, skip: Boolean)
}

@Dao
interface AlarmInstanceDao {
    @Query("SELECT * FROM alarm_instances ORDER BY firesAtEpochMillis")
    fun observeAll(): Flow<List<AlarmInstanceEntity>>

    @Query("SELECT * FROM alarm_instances ORDER BY firesAtEpochMillis")
    suspend fun getAll(): List<AlarmInstanceEntity>

    @Query("SELECT * FROM alarm_instances WHERE id = :id")
    suspend fun get(id: Long): AlarmInstanceEntity?

    @Query("SELECT * FROM alarm_instances WHERE alarmId = :alarmId")
    suspend fun getForAlarm(alarmId: Long): List<AlarmInstanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instance: AlarmInstanceEntity): Long

    @Update
    suspend fun update(instance: AlarmInstanceEntity)

    @Delete
    suspend fun delete(instance: AlarmInstanceEntity)

    @Query("DELETE FROM alarm_instances WHERE alarmId = :alarmId")
    suspend fun deleteForAlarm(alarmId: Long)

    @Query("DELETE FROM alarm_instances")
    suspend fun deleteAll()
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY createdAtMillis, id")
    fun observeAll(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers ORDER BY createdAtMillis, id")
    suspend fun getAll(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun get(id: Long): TimerEntity?

    @Upsert
    suspend fun upsert(timer: TimerEntity): Long

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LapDao {
    @Query("SELECT * FROM laps ORDER BY number DESC")
    fun observeAll(): Flow<List<LapEntity>>

    @Query("SELECT COUNT(*) FROM laps")
    suspend fun count(): Int

    @Insert
    suspend fun insert(lap: LapEntity)

    @Query("DELETE FROM laps")
    suspend fun deleteAll()
}
