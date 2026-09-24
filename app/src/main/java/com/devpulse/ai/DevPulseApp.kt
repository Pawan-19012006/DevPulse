package com.devpulse.ai

import android.app.Application
import com.devpulse.ai.data.local.DevPulseDatabase
import com.devpulse.ai.domain.session.SessionEngine
import com.devpulse.ai.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DevPulseApp : Application() {
    lateinit var database: DevPulseDatabase
        private set
    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var sessionEngine: SessionEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = DevPulseDatabase.getInstance(this)
        sessionRepository = SessionRepository(database)
        sessionEngine = SessionEngine(database = database)

        // Seed default pre-session checklist and recovery activities asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            sessionRepository.ensureDefaultsSeeded()
        }
    }

    companion object {
        lateinit var instance: DevPulseApp
            private set
    }
}
