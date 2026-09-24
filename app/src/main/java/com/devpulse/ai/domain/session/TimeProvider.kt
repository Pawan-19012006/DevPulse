package com.devpulse.ai.domain.session

interface TimeProvider {
    fun now(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
