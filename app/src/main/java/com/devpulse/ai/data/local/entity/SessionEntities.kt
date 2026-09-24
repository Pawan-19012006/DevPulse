package com.devpulse.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dev_sessions",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["status"])
    ]
)
data class DevSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val activityType: String,
    val goal: String? = null,
    val targetDurationMinutes: Int = 25,
    val actualDurationMinutes: Int = 0,
    val initialState: String? = null,
    val finalState: String? = null,
    val status: String = "IN_PROGRESS",
    val startedAt: Long,
    val completedAt: Long? = null,
    // Phase 2 additions:
    val sessionMode: String = "FOCUS", // "FOCUS" or "DEEP_WORK"
    val currentObjective: String = goal ?: "Focused Work",
    val workBlockDurationMinutes: Int = 25,
    val recoveryBlockDurationMinutes: Int = 5,
    val totalBlocks: Int = 1,
    val currentBlockIndex: Int = 0,
    val currentMindset: String? = initialState
)

@Entity(
    tableName = "session_blocks",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["status"])
    ]
)
data class SessionBlockEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val blockIndex: Int,
    val blockType: String, // "WORK" or "RECOVERY"
    val objective: String,
    val plannedDurationSeconds: Long,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val totalPausedDurationMs: Long = 0L,
    val completedAt: Long? = null,
    val status: String = "WORKING" // "WORKING", "PAUSED", "COMPLETED"
)

@Entity(
    tableName = "session_handoffs",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["isUnfinished"]),
        Index(value = ["createdAt"])
    ]
)
data class SessionHandoffEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val blockIndex: Int,
    val sessionGoal: String,
    val accomplished: String,
    val nextObjective: String,
    val isUnfinished: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "one_percent_improvements",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["sessionId"]),
        Index(value = ["category"])
    ]
)
data class OnePercentImprovementEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val category: String,
    val reflectionText: String,
    val timestamp: Long
)

@Entity(
    tableName = "pre_session_checklist",
    indices = [Index(value = ["sortOrder"])]
)
data class PreSessionChecklistItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isDefault: Boolean = true,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "recovery_activities",
    indices = [Index(value = ["category"])]
)
data class RecoveryActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // PHYSICAL, MENTAL, HYDRATION, CUSTOM
    val durationMinutes: Int = 5,
    val isDefault: Boolean = true
)
