package com.devpulse.ai.domain.quotes

import java.util.Calendar

data class DevPulseQuote(
    val text: String,
    val source: String? = null
)

object DailyQuotes {
    private val quotes = listOf(
        DevPulseQuote("Don't measure the day by how much you finished. Measure it by what you understood."),
        DevPulseQuote("Every difficult problem is an opportunity to become a better engineer."),
        DevPulseQuote("You don't need to finish everything today. You just need to move one thing forward."),
        DevPulseQuote("Simplicity is prerequisite for reliability.", "Edsger W. Dijkstra"),
        DevPulseQuote("Clear thinking precedes clean code."),
        DevPulseQuote("When debugging stalls, a short reset away from the screen often surfaces the missing assumption."),
        DevPulseQuote("Code is read much more often than it is written.", "Guido van Rossum"),
        DevPulseQuote("Quality is not an act, it is a habit built one session at a time."),
        DevPulseQuote("Make it work, make it right, make it fast.", "Kent Beck"),
        DevPulseQuote("The mind solves deep architectural problems in the quiet spaces between focus."),
        DevPulseQuote("Focus on the next line, the next test, the next single step."),
        DevPulseQuote("Premature optimization is the root of all evil.", "Donald Knuth"),
        DevPulseQuote("You showed up. That is where all meaningful progress begins."),
        DevPulseQuote("Sustainable building beats sporadic heroics every time."),
        DevPulseQuote("Give yourself permission to write imperfect code first, then refine it with care.")
    )

    fun getQuoteForToday(calendar: Calendar = Calendar.getInstance()): DevPulseQuote {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        val index = ((dayOfYear * 31 + year) % quotes.size).let { if (it < 0) it + quotes.size else it }
        return quotes[index]
    }
}
