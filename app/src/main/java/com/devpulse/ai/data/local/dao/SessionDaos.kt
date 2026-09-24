package com.devpulse.ai.data.local.dao

import androidx.room.*
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.RecoveryActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevSessionDao {
    @Upsert
    suspend fun upsertSession(session: DevSessionEntity)

    @Query("SELECT * FROM dev_sessions ORDER BY startedAt DESC")
    fun observeAllSessions(): Flow<List<DevSessionEntity>>

    @Query("SELECT * FROM dev_sessions WHERE startedAt >= :startTime AND startedAt <= :endTime ORDER BY startedAt DESC")
    fun observeSessionsBetween(startTime: Long, endTime: Long): Flow<List<DevSessionEntity>>

    @Query("SELECT COUNT(*) FROM dev_sessions")
    fun observeTotalSessionsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dev_sessions WHERE startedAt >= :startTime AND startedAt <= :endTime")
    fun observeSessionsCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT * FROM dev_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): DevSessionEntity?
}

@Dao
interface OnePercentImprovementDao {
    @Upsert
    suspend fun upsertImprovement(improvement: OnePercentImprovementEntity)

    @Query("SELECT * FROM one_percent_improvements ORDER BY timestamp DESC")
    fun observeAllImprovements(): Flow<List<OnePercentImprovementEntity>>

    @Query("SELECT * FROM one_percent_improvements ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentImprovements(limit: Int = 10): Flow<List<OnePercentImprovementEntity>>

    @Query("SELECT COUNT(*) FROM one_percent_improvements")
    fun observeTotalImprovementsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM one_percent_improvements WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun observeImprovementsCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT * FROM one_percent_improvements WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getImprovementForSession(sessionId: String): OnePercentImprovementEntity?
}

@Dao
interface PreSessionChecklistDao {
    @Upsert
    suspend fun upsertChecklistItems(items: List<PreSessionChecklistItemEntity>)

    @Query("SELECT * FROM pre_session_checklist ORDER BY sortOrder ASC")
    fun observeChecklist(): Flow<List<PreSessionChecklistItemEntity>>

    @Query("SELECT COUNT(*) FROM pre_session_checklist")
    suspend fun getChecklistCount(): Int

    @Query("UPDATE pre_session_checklist SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateItemEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM pre_session_checklist WHERE id = :id")
    suspend fun deleteItem(id: String)
}

@Dao
interface RecoveryActivityDao {
    @Upsert
    suspend fun upsertActivities(items: List<RecoveryActivityEntity>)

    @Query("SELECT * FROM recovery_activities ORDER BY isDefault DESC, title ASC")
    fun observeActivities(): Flow<List<RecoveryActivityEntity>>

    @Query("SELECT COUNT(*) FROM recovery_activities")
    suspend fun getActivityCount(): Int

    @Query("DELETE FROM recovery_activities WHERE id = :id")
    suspend fun deleteActivity(id: String)
}
