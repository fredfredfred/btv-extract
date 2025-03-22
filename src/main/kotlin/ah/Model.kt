package ah

import java.time.LocalDate
import java.time.LocalTime

data class DaySection(
    val day: String,
    val section: String,
)

val homeTeamNames = setOf("Herren", "Damen", "Knaben", "Mädchen", "Dunlop", "Bambini", "Junioren", "Juniorinnen")

data class Game(
    val day: String,
    val time: String,
    val league: String,
    val homeTeam: String,
    val guestTeam: String,
    val isHome: Boolean = homeTeamNames.any { homeTeam.contains(it) },
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
    fun toCsv(separator: String = ","): String {
        val startTimeStr = startTime.format(usTimePattern)
        val endTimeStr = endTime.format(usTimePattern)
        return "${subject}$separator ${startDate}$separator ${startTimeStr}$separator ${endDate}$separator ${endTimeStr}$separator ${allDayEvent}$separator ${description}$separator ${location}"
    }
}

