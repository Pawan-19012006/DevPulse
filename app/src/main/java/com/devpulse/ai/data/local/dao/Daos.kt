package com.devpulse.ai.data.local.dao

import androidx.room.*
import com.devpulse.ai.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeveloperProfileDao {
    @Upsert
    suspend fun upsertProfile(profile: DeveloperProfileEntity)

    @Query("SELECT * FROM developer_profiles WHERE username = :username LIMIT 1")
    suspend fun getProfile(username: String): DeveloperProfileEntity?

    @Query("SELECT * FROM developer_profiles WHERE username = :username LIMIT 1")
    fun observeProfile(username: String): Flow<DeveloperProfileEntity?>
}

@Dao
interface RepositoryDao {
    @Upsert
    suspend fun upsertRepositories(repos: List<RepositoryEntity>)

    @Query("SELECT * FROM repositories WHERE owner = :owner ORDER BY stargazersCount DESC")
    suspend fun getRepositoriesForOwner(owner: String): List<RepositoryEntity>

    @Query("SELECT * FROM repositories WHERE owner = :owner ORDER BY stargazersCount DESC LIMIT :limit")
    suspend fun getTopRepositories(owner: String, limit: Int = 5): List<RepositoryEntity>

    @Query("SELECT * FROM repositories WHERE owner = :owner ORDER BY pushedAt DESC")
    suspend fun getRecentlyPushed(owner: String): List<RepositoryEntity>

    @Query("SELECT COUNT(*) FROM repositories WHERE owner = :owner")
    suspend fun getRepositoryCount(owner: String): Int

    @Query("SELECT SUM(stargazersCount) FROM repositories WHERE owner = :owner")
    suspend fun getTotalStars(owner: String): Int?

    @Query("SELECT SUM(forksCount) FROM repositories WHERE owner = :owner")
    suspend fun getTotalForks(owner: String): Int?
}

@Dao
interface DeveloperEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<DeveloperEventEntity>): List<Long>

    @Query("SELECT * FROM developer_events WHERE developerUsername = :username ORDER BY timestamp DESC")
    suspend fun getEventsForDeveloper(username: String): List<DeveloperEventEntity>

    @Query("SELECT * FROM developer_events WHERE developerUsername = :username AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getEventsBetween(username: String, startTime: Long, endTime: Long): List<DeveloperEventEntity>

    @Query("SELECT * FROM developer_events WHERE developerUsername = :username AND repositoryName = :repo ORDER BY timestamp DESC")
    suspend fun getEventsByRepo(username: String, repo: String): List<DeveloperEventEntity>

    @Query("SELECT COUNT(*) FROM developer_events WHERE developerUsername = :username AND eventType = :eventType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun countEventsByTypeBetween(username: String, eventType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') FROM developer_events WHERE developerUsername = :username AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getActiveDaysBetween(username: String, startTime: Long, endTime: Long): List<String>
}

@Dao
interface ActivitySnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ActivitySnapshotEntity)

    @Query("SELECT * FROM activity_snapshots WHERE developerUsername = :username ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(username: String): ActivitySnapshotEntity?

    @Query("SELECT * FROM activity_snapshots WHERE developerUsername = :username AND timestamp <= :timestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun getSnapshotAtOrBefore(username: String, timestamp: Long): ActivitySnapshotEntity?

    @Query("SELECT * FROM activity_snapshots WHERE developerUsername = :username ORDER BY timestamp DESC")
    suspend fun getAllSnapshots(username: String): List<ActivitySnapshotEntity>
}

@Dao
interface SkillEvidenceDao {
    @Upsert
    suspend fun upsertSkills(skills: List<SkillEvidenceEntity>)

    @Query("SELECT * FROM skill_evidences WHERE developerUsername = :username ORDER BY evidenceCount DESC")
    suspend fun getSkillsForDeveloper(username: String): List<SkillEvidenceEntity>

    @Query("DELETE FROM skill_evidences WHERE developerUsername = :username")
    suspend fun deleteSkillsForDeveloper(username: String)
}
