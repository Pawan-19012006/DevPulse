package com.devpulse.ai.domain.session

import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.SessionBlockEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity

enum class SessionMode(val displayName: String, val description: String) {
    FOCUS("Focus Session", "Single-block focus session with post-session handoff"),
    DEEP_WORK("Deep Work", "Multi-block session with structured recovery breaks")
}

enum class BlockType {
    WORK,
    RECOVERY
}

enum class BlockStatus {
    WORKING,
    PAUSED,
    COMPLETED
}

/**
 * Pure domain representation of a session block.
 */
data class SessionBlock(
    val id: String,
    val sessionId: String,
    val blockIndex: Int,
    val blockType: BlockType,
    val objective: String,
    val plannedDurationSeconds: Long,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val totalPausedDurationMs: Long = 0L,
    val completedAt: Long? = null,
    val status: BlockStatus = BlockStatus.WORKING
) {
    fun calculateElapsedSeconds(currentTimeMs: Long): Long {
        if (status == BlockStatus.COMPLETED && completedAt != null) {
            val paused = totalPausedDurationMs
            return ((completedAt - startedAt - paused).coerceAtLeast(0L) / 1000L)
        }
        val activePause = if (pausedAt != null) (currentTimeMs - pausedAt).coerceAtLeast(0L) else 0L
        val totalPause = totalPausedDurationMs + activePause
        val elapsedMs = (currentTimeMs - startedAt - totalPause).coerceAtLeast(0L)
        return elapsedMs / 1000L
    }

    fun calculateRemainingSeconds(currentTimeMs: Long): Long {
        val elapsed = calculateElapsedSeconds(currentTimeMs)
        return (plannedDurationSeconds - elapsed).coerceAtLeast(0L)
    }

    fun calculateProgressRatio(currentTimeMs: Long): Float {
        if (plannedDurationSeconds <= 0L) return 1.0f
        val elapsed = calculateElapsedSeconds(currentTimeMs)
        return (elapsed.toFloat() / plannedDurationSeconds.toFloat()).coerceIn(0.0f, 1.0f)
    }

    fun isTimeFinished(currentTimeMs: Long): Boolean {
        return calculateRemainingSeconds(currentTimeMs) <= 0L
    }
}

/**
 * Pure domain representation of an ongoing or completed DevPulse session.
 */
data class DevSession(
    val id: String,
    val title: String,
    val sessionMode: SessionMode,
    val activityType: SessionActivityType,
    val overallGoal: String,
    val currentObjective: String,
    val initialMindset: DeveloperState?,
    val currentMindset: DeveloperState?,
    val totalPlannedDurationMinutes: Int,
    val workBlockDurationMinutes: Int,
    val recoveryBlockDurationMinutes: Int,
    val totalBlocks: Int,
    val currentBlockIndex: Int,
    val status: SessionStatus,
    val startedAt: Long,
    val completedAt: Long? = null
)

/**
 * Presets for Deep Work sessions.
 */
data class DeepWorkPreset(
    val label: String,
    val totalHours: Int,
    val workMinutes: Int,
    val recoveryMinutes: Int,
    val numberOfBlocks: Int
)

object DeepWorkPresets {
    val TWO_HOURS = DeepWorkPreset("2 Hours", 2, 50, 10, 2)
    val THREE_HOURS = DeepWorkPreset("3 Hours", 3, 45, 15, 3)
    val FOUR_HOURS = DeepWorkPreset("4 Hours", 4, 45, 15, 4)

    val ALL = listOf(TWO_HOURS, THREE_HOURS, FOUR_HOURS)
}

/**
 * Single source of truth state machine for the DevPulse Session Engine.
 */
sealed interface SessionEngineState {
    object Idle : SessionEngineState

    data class Preparing(
        val session: DevSession,
        val firstBlock: SessionBlock,
        val unfinishedHandoff: SessionHandoffEntity? = null
    ) : SessionEngineState

    data class Working(
        val session: DevSession,
        val currentBlock: SessionBlock,
        val elapsedSeconds: Long,
        val remainingSeconds: Long,
        val progressRatio: Float
    ) : SessionEngineState {
        val block: SessionBlock get() = currentBlock
    }

    data class Paused(
        val session: DevSession,
        val currentBlock: SessionBlock,
        val elapsedSeconds: Long,
        val remainingSeconds: Long,
        val progressRatio: Float
    ) : SessionEngineState {
        val block: SessionBlock get() = currentBlock
    }

    data class WorkBlockHandoff(
        val session: DevSession,
        val completedBlock: SessionBlock,
        val isFinalBlock: Boolean
    ) : SessionEngineState

    data class Recovery(
        val session: DevSession,
        val recoveryBlock: SessionBlock,
        val nextObjective: String,
        val elapsedSeconds: Long,
        val remainingSeconds: Long,
        val progressRatio: Float
    ) : SessionEngineState

    data class ReadyForNextBlock(
        val session: DevSession,
        val nextBlockIndex: Int,
        val carryoverObjective: String
    ) : SessionEngineState

    data class SessionCompleteReflection(
        val session: DevSession,
        val lastHandoff: SessionHandoffEntity?
    ) : SessionEngineState

    data class Finished(
        val sessionId: String,
        val improvement: OnePercentImprovementEntity?
    ) : SessionEngineState
}
