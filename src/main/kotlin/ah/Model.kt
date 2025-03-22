package ah

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DaySection(
    val day: String,
    val section: String,
)

data class Game(
    val day: String,
    val time: String,
    val league: String,
    val homeTeam: String,
    val guestTeam: String,
    val isHome: Boolean = homeTeamStarters.any { homeTeam.contains(it) },
)

data class CsvGoogleCalendar(
    val header: List<String> = listOf(
        "Subject",
        "Start Date",
        "Start Time",
        "End Date",
        "End Time",
        "All Day Event",
        "Description",
        "Location"
    ),
    val matches: List<CsvGame>,
    val separator: String = ","
)


data class CsvGame(
    val subject: String,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val allDayEvent: Boolean = false,
    val description: String,
    val location: String,
) {
    private val usTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun toCsv(separator: String = ","): String {
        val startTimeStr = startTime.format(usTimePattern)
        val endTimeStr = endTime.format(usTimePattern)
        return "${subject}$separator ${startDate}$separator ${startTimeStr}$separator ${endDate}$separator ${endTimeStr}$separator ${allDayEvent}$separator ${description}$separator ${location}"
    }
}

