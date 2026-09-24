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
    val status: String = "COMPLETED",
    val startedAt: Long,
    val completedAt: Long? = null
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
