package com.devpulse.ai.data.local.dao

import androidx.room.*
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.RecoveryActivityEntity
import com.devpulse.ai.data.local.entity.SessionBlockEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
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

    @Query("SELECT * FROM dev_sessions WHERE status IN ('IN_PROGRESS', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): DevSessionEntity?

    @Query("SELECT * FROM dev_sessions WHERE status IN ('IN_PROGRESS', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<DevSessionEntity?>

    @Query("UPDATE dev_sessions SET currentBlockIndex = :blockIndex, currentObjective = :objective WHERE id = :sessionId")
    suspend fun updateCurrentBlock(sessionId: String, blockIndex: Int, objective: String)

    @Query("UPDATE dev_sessions SET currentMindset = :mindset WHERE id = :sessionId")
    suspend fun updateMindset(sessionId: String, mindset: String)

    @Query("UPDATE dev_sessions SET status = :status, completedAt = :completedAt, actualDurationMinutes = :actualDurationMinutes WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: String, status: String, completedAt: Long?, actualDurationMinutes: Int)
}

@Dao
interface SessionBlockDao {
    @Upsert
    suspend fun upsertBlock(block: SessionBlockEntity)

    @Upsert
    suspend fun upsertBlocks(blocks: List<SessionBlockEntity>)

    @Query("SELECT * FROM session_blocks WHERE id = :id LIMIT 1")
    suspend fun getBlockById(id: String): SessionBlockEntity?

    @Query("SELECT * FROM session_blocks WHERE sessionId = :sessionId AND status IN ('WORKING', 'PAUSED') ORDER BY blockIndex DESC LIMIT 1")
    suspend fun getActiveBlock(sessionId: String): SessionBlockEntity?

    @Query("SELECT * FROM session_blocks WHERE sessionId = :sessionId AND status IN ('WORKING', 'PAUSED') ORDER BY blockIndex DESC LIMIT 1")
    fun observeActiveBlock(sessionId: String): Flow<SessionBlockEntity?>

    @Query("SELECT * FROM session_blocks WHERE sessionId = :sessionId ORDER BY blockIndex ASC")
    suspend fun getBlocksForSession(sessionId: String): List<SessionBlockEntity>

    @Query("SELECT * FROM session_blocks WHERE sessionId = :sessionId ORDER BY blockIndex ASC")
    fun observeBlocksForSession(sessionId: String): Flow<List<SessionBlockEntity>>

    @Query("UPDATE session_blocks SET pausedAt = :pausedAt, totalPausedDurationMs = :totalPausedDurationMs, status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateBlockState(id: String, pausedAt: Long?, totalPausedDurationMs: Long, status: String, completedAt: Long?)
}

@Dao
interface SessionHandoffDao {
    @Upsert
    suspend fun upsertHandoff(handoff: SessionHandoffEntity)

    @Query("SELECT * FROM session_handoffs WHERE isUnfinished = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestUnfinishedHandoff(): SessionHandoffEntity?

    @Query("SELECT * FROM session_handoffs WHERE isUnfinished = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestUnfinishedHandoff(): Flow<SessionHandoffEntity?>

    @Query("UPDATE session_handoffs SET isUnfinished = 0 WHERE id = :id")
    suspend fun markHandoffCompleted(id: String)

    @Query("SELECT * FROM session_handoffs WHERE sessionId = :sessionId ORDER BY blockIndex ASC")
    suspend fun getHandoffsForSession(sessionId: String): List<SessionHandoffEntity>

    @Query("SELECT * FROM session_handoffs WHERE sessionId = :sessionId ORDER BY blockIndex ASC")
    fun observeHandoffsForSession(sessionId: String): Flow<List<SessionHandoffEntity>>
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
