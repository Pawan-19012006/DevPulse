package com.devpulse.ai.domain.coach

object CoachFallbackEngine {

    /**
     * Generates grounded, rule-based developer wellness guidance using the exact same DevPulse context.
     * Guaranteed never to throw, fail, or make medical claims.
     */
    fun generateGuidance(context: DeveloperContextSummary): CoachInsight {
        val checkIn = context.latestWellnessCheck?.uppercase() ?: ""
        val isStressed = checkIn.contains("STRESSED")
        val isTired = checkIn.contains("TIRED")
        val isEnergized = checkIn.contains("ENERGIZED")

        // CASE 2: High Stress Check-in
        if (isStressed) {
            return CoachInsight(
                title = "Decompress your nervous system",
                observation = "Your check-in indicates elevated stress or cognitive overload right now.",
                guidance = "Don't add another demanding task immediately. Take a 2-minute breathing reset first to stabilize your focus.",
                action = CoachAction.START_RECOVERY,
                actionLabel = "Start Box Breathing",
                isFromAi = false
            )
        }

        // CASE 1: Long Work Session Without Recovery
        if ((context.totalFocusMinutesToday >= 45 || context.sessionsToday >= 2) && context.recoverySessionsToday == 0) {
            return CoachInsight(
                title = "Time for a recovery reset",
                observation = "You've completed ${context.sessionsToday} session(s) (${context.totalFocusMinutesToday} min) with no recovery breaks today.",
                guidance = "Your next step doesn't need to be another long session. Give your eyes and posture a 2-minute reset, then continue.",
                action = CoachAction.START_RECOVERY,
                actionLabel = "Start Recovery",
                isFromAi = false
            )
        }

        // CASE 3: Low Energy / Tired
        if (isTired) {
            val ship = context.currentDailyShip ?: "your objective"
            return CoachInsight(
                title = "Keep today's scope small",
                observation = "Your stamina is dipping after your recent coding blocks.",
                guidance = "Instead of starting something new, finish only the smallest remaining part of \"$ship\".",
                action = CoachAction.CONTINUE_DAILY_SHIP,
                actionLabel = "Continue Daily Ship",
                isFromAi = false
            )
        }

        // CASE 4: Good Progress Already Made Today
        if (context.recentOnePercentImprovements.isNotEmpty() && context.sessionsToday >= 1) {
            val latestShip = context.recentOnePercentImprovements.first()
            return CoachInsight(
                title = "Meaningful progress achieved",
                observation = "You already logged a 1% Better improvement: \"$latestShip\".",
                guidance = "You don't need to force extra sessions today. If fatigue is setting in, taking a recovery break is healthy and sustainable.",
                action = CoachAction.START_RECOVERY,
                actionLabel = "Start Recovery",
                isFromAi = false
            )
        }

        // CASE 5: Long-term Goal Momentum
        if (context.meaningfulDays >= 5 && isEnergized) {
            return CoachInsight(
                title = "Steady journey progress",
                observation = "You're ${context.meaningfulDays} meaningful days into your \"${context.currentGoal}\" journey.",
                guidance = "Your momentum is strong. Channel this energy into today's core architectural problem.",
                action = CoachAction.CONTINUE_DAILY_SHIP,
                actionLabel = "Continue Daily Ship",
                isFromAi = false
            )
        }

        // CASE 6: Minimal Activity / Fresh Start
        if (context.sessionsToday == 0) {
            val target = context.currentDailyShip ?: context.currentGoal
            return CoachInsight(
                title = "Start with a single clear block",
                observation = "No focus sessions have been completed yet today.",
                guidance = "Start your day with one 25-minute deep focus block targeting \"$target\".",
                action = CoachAction.START_FOCUS_SESSION,
                actionLabel = "Start Focus Session",
                isFromAi = false
            )
        }

        // Default Balanced State
        return CoachInsight(
            title = "Sustainable development pace",
            observation = "You have ${context.sessionsToday} session(s) and ${context.recoverySessionsToday} recovery break(s) logged today.",
            guidance = "Maintain your rhythm by alternating between deliberate deep work and timely resets.",
            action = CoachAction.CONTINUE_DAILY_SHIP,
            actionLabel = "Continue Daily Ship",
            isFromAi = false
        )
    }

    /**
     * Answers targeted developer questions deterministically if Gemini is unavailable.
     */
    fun answerQuestion(question: String, context: DeveloperContextSummary): CoachInsight {
        val q = question.lowercase()
        return when {
            q.contains("break") || q.contains("rest") -> {
                if (context.totalFocusMinutesToday >= 40 && context.recoverySessionsToday == 0) {
                    CoachInsight(
                        title = "Take a recovery break",
                        observation = "You have ${context.totalFocusMinutesToday} minutes of focus logged with zero recovery breaks.",
                        guidance = "Yes. A 2-minute eye and posture break right now will restore cognitive clarity for your next code challenge.",
                        action = CoachAction.START_RECOVERY,
                        actionLabel = "Start Recovery",
                        isFromAi = false
                    )
                } else {
                    CoachInsight(
                        title = "Pacing check",
                        observation = "You've balanced ${context.sessionsToday} session(s) with ${context.recoverySessionsToday} recovery break(s).",
                        guidance = "If you feel your attention wandering or eyes straining, step away for 2 minutes. Otherwise, complete your current block.",
                        action = CoachAction.START_RECOVERY,
                        actionLabel = "Start Recovery",
                        isFromAi = false
                    )
                }
            }
            q.contains("next") || q.contains("focus") -> {
                val ship = context.currentDailyShip
                if (!ship.isNullOrBlank()) {
                    CoachInsight(
                        title = "Next: Daily Ship",
                        observation = "Your active Daily Ship is \"$ship\".",
                        guidance = "Tackle the single most immediate block: verify edge cases or run tests on this objective.",
                        action = CoachAction.CONTINUE_DAILY_SHIP,
                        actionLabel = "Continue Daily Ship",
                        isFromAi = false
                    )
                } else {
                    CoachInsight(
                        title = "Define your next step",
                        observation = "No active Daily Ship objective is currently defined.",
                        guidance = "Pick the single smallest piece of work that advances your \"${context.currentGoal}\" journey and start a focus session.",
                        action = CoachAction.START_FOCUS_SESSION,
                        actionLabel = "Start Focus Session",
                        isFromAi = false
                    )
                }
            }
            q.contains("progress") || q.contains("constellation") || q.contains("goal") -> {
                CoachInsight(
                    title = "Your Constellation Progress",
                    observation = "You have accumulated ${context.meaningfulDays} of ${context.targetDays} meaningful days.",
                    guidance = "Every 1% Better reflection adds a star to your constellation. Consistency beats marathon sessions.",
                    action = CoachAction.VIEW_TRACKER,
                    actionLabel = "View Constellation",
                    isFromAi = false
                )
            }
            q.contains("scope") || q.contains("reduce") || q.contains("overwhelm") -> {
                CoachInsight(
                    title = "Scope Reduction Strategy",
                    observation = "Large tasks cause cognitive friction and analysis paralysis.",
                    guidance = "Cut today's goal in half. Then cut it again until it is just one function, one test, or one UI component you can finish in 25 minutes.",
                    action = CoachAction.CONTINUE_DAILY_SHIP,
                    actionLabel = "Continue Daily Ship",
                    isFromAi = false
                )
            }
            else -> {
                CoachInsight(
                    title = "Coach Recommendation",
                    observation = "Analyzing your ${context.sessionsToday} session(s) toward \"${context.currentGoal}\".",
                    guidance = "Prioritize sustainable rhythm over volume. Focus on finishing one small thing that makes you 1% better today.",
                    action = CoachAction.CONTINUE_DAILY_SHIP,
                    actionLabel = "Continue Work",
                    isFromAi = false
                )
            }
        }
    }

    /**
     * Generates a conversational message reply grounded in developer context.
     */
    fun generateConversationalReply(
        userMessage: String,
        context: DeveloperContextSummary,
        conversationHistory: List<CoachChatMessage> = emptyList()
    ): CoachChatMessage {
        val q = userMessage.lowercase().trim()

        // 1. Fatigue / Exhaustion / Long coding session
        if (q.contains("exhausted") || q.contains("tired") || q.contains("coding for") || q.contains("3 hours") || q.contains("long time") || q.contains("burnout")) {
            val focusDescription = if (context.totalFocusMinutesToday > 0) {
                "${context.totalFocusMinutesToday} minutes of focus today"
            } else {
                "extended focus"
            }
            val recoveryNote = if (context.recoverySessionsToday == 0) "limited recovery" else "only ${context.recoverySessionsToday} recovery break(s)"
            val shipMention = context.currentDailyShip?.let { "When you return, continue the smallest remaining part of \"$it\"." } ?: "When you return, pick one small step to finish."

            return CoachChatMessage(
                sender = MessageSender.COACH,
                text = "Given your $focusDescription and $recoveryNote, I'd avoid starting another demanding task right now.\n\nTake a short recovery break first. $shipMention",
                action = CoachAction.START_RECOVERY,
                actionLabel = "Start Recovery"
            )
        }

        // 2. Wanting to push through / finish task despite fatigue
        if (q.contains("finish my task") || q.contains("finish the task") || q.contains("really want to finish") || q.contains("keep coding") || q.contains("want to finish")) {
            val ship = context.currentDailyShip ?: "your current objective"
            return CoachChatMessage(
                sender = MessageSender.COACH,
                text = "Then don't abandon it. Reduce the scope instead. Your current Daily Ship is \"$ship\", so finish the smallest remaining piece rather than starting another task.",
                action = CoachAction.CONTINUE_DAILY_SHIP,
                actionLabel = "Continue Daily Ship"
            )
        }

        // 3. Should I take a break?
        if (q.contains("break") || q.contains("rest") || q.contains("pause")) {
            return if (context.totalFocusMinutesToday >= 45 || context.recoverySessionsToday == 0) {
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "Yes, step away now. You've logged ${context.totalFocusMinutesToday} focus minutes today with ${if (context.recoverySessionsToday == 0) "no recovery breaks" else "${context.recoverySessionsToday} break"}. A 2-minute eye or movement reset will restore your concentration.",
                    action = CoachAction.START_RECOVERY,
                    actionLabel = "Start Recovery"
                )
            } else {
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "You've taken ${context.recoverySessionsToday} recovery break(s) today. If your concentration is slipping or eyes feel dry, take a short reset; otherwise, complete your active block.",
                    action = CoachAction.START_RECOVERY,
                    actionLabel = "Start Recovery"
                )
            }
        }

        // 4. What should I work on next?
        if (q.contains("what should i work on") || q.contains("what to do") || q.contains("next") || q.contains("work on next")) {
            val ship = context.currentDailyShip
            return if (!ship.isNullOrBlank()) {
                val shipStatus = context.dailyShipStatus ?: "In Progress"
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "Look at your active Daily Ship: \"$ship\" ($shipStatus). Keep advancing toward your goal of \"${context.currentGoal}\" by finishing the next small testable increment.",
                    action = CoachAction.CONTINUE_DAILY_SHIP,
                    actionLabel = "Continue Daily Ship"
                )
            } else {
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "You don't have an active Daily Ship set. Pick the smallest, most concrete piece of work that advances your long-term goal: \"${context.currentGoal}\".",
                    action = CoachAction.START_FOCUS_SESSION,
                    actionLabel = "Start Focus Session"
                )
            }
        }

        // 5. Am I overworking?
        if (q.contains("overworking") || q.contains("working too much") || q.contains("too much")) {
            return if (context.totalFocusMinutesToday >= 90 || context.sessionsToday >= 3) {
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "Your activity today is relatively high (${context.totalFocusMinutesToday} focus minutes across ${context.sessionsToday} sessions) with ${context.recoverySessionsToday} recovery breaks. Consider slowing down and letting your brain process what you've built.",
                    action = CoachAction.START_RECOVERY,
                    actionLabel = "Start Recovery"
                )
            } else {
                CoachChatMessage(
                    sender = MessageSender.COACH,
                    text = "Your current pacing looks sustainable with ${context.sessionsToday} session(s) (${context.totalFocusMinutesToday} minutes). Remember that consistent moderate pacing beats high-stress marathons.",
                    action = CoachAction.NONE
                )
            }
        }

        // 6. How is my progress? / How am I doing?
        if (q.contains("how am i doing") || q.contains("how is my progress") || q.contains("progress") || q.contains("constellation")) {
            val recentImprovement = context.recentOnePercentImprovements.firstOrNull()
            val improvementNote = if (recentImprovement != null) " Recent win: \"$recentImprovement\"." else ""
            return CoachChatMessage(
                sender = MessageSender.COACH,
                text = "You've accumulated ${context.meaningfulDays} of ${context.targetDays} meaningful days toward \"${context.currentGoal}\".$improvementNote Every 1% Better reflection adds a star to your constellation.",
                action = CoachAction.VIEW_TRACKER,
                actionLabel = "View Constellation"
            )
        }

        // 7. General fallback grounded in developer context
        val goal = context.currentGoal
        val ship = context.currentDailyShip
        return CoachChatMessage(
            sender = MessageSender.COACH,
            text = if (ship != null) {
                "Keep your focus simple. You're working toward \"$goal\" and your current objective is \"$ship\". Make steady progress in small increments, and remember to rest when your focus begins to fade."
            } else {
                "Sustainable progress toward \"$goal\" comes from steady pacing rather than excessive hours. Focus on one small milestone at a time."
            },
            action = CoachAction.CONTINUE_DAILY_SHIP,
            actionLabel = "Continue Daily Ship"
        )
    }
}

