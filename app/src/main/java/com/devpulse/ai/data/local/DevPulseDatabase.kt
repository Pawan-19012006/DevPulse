package com.devpulse.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devpulse.ai.data.local.dao.*
import com.devpulse.ai.data.local.entity.*

@Database(
    entities = [
        DeveloperProfileEntity::class,
        RepositoryEntity::class,
        DeveloperEventEntity::class,
        ActivitySnapshotEntity::class,
        SkillEvidenceEntity::class,
        DevSessionEntity::class,
        SessionBlockEntity::class,
        SessionHandoffEntity::class,
        OnePercentImprovementEntity::class,
        PreSessionChecklistItemEntity::class,
        RecoveryActivityEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DevPulseDatabase : RoomDatabase() {

    abstract fun developerProfileDao(): DeveloperProfileDao
    abstract fun repositoryDao(): RepositoryDao
    abstract fun developerEventDao(): DeveloperEventDao
    abstract fun activitySnapshotDao(): ActivitySnapshotDao
    abstract fun skillEvidenceDao(): SkillEvidenceDao
    abstract fun devSessionDao(): DevSessionDao
    abstract fun sessionBlockDao(): SessionBlockDao
    abstract fun sessionHandoffDao(): SessionHandoffDao
    abstract fun onePercentImprovementDao(): OnePercentImprovementDao
    abstract fun preSessionChecklistDao(): PreSessionChecklistDao
    abstract fun recoveryActivityDao(): RecoveryActivityDao

    companion object {
        @Volatile
        private var INSTANCE: DevPulseDatabase? = null

        fun getInstance(context: Context): DevPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevPulseDatabase::class.java,
                    "devpulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
