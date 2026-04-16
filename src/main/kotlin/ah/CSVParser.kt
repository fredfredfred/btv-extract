package ah

import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object CSVParser {
    private val localDatePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val localTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private const val TC_DORFEN = "TC Dorfen"
    // Youth categories that always play in (4er) format but BTV CSV doesn't include it in Liga column
    private val always4er = setOf("Junioren 18", "Juniorinnen 18", "Bambini 12", "Dunlop Midcourt U10", "Dunlop Kleinfeld U9")

    fun parseGamesCsv(inputFile: File): Pair<List<CsvGame>, List<Game>> {
        val lines = inputFile.readLines(Charset.forName("ISO-8859-1"))
        if (lines.size < 2) return emptyList<CsvGame>() to emptyList()

        val games = lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val fields = line.split(";")
            val spieltermin = fields[0]
            val uhrzeit = fields[1]
            val altersklasse = fields[2]
            val liga = fields[4]
            val heimmannschaft = fields[6]
            val gastmannschaft = fields[7]

            val isHome = heimmannschaft.startsWith(TC_DORFEN)
            val dorfenTeam = if (isHome) heimmannschaft else gastmannschaft
            val opponent = if (isHome) gastmannschaft else heimmannschaft

            val teamSuffix = dorfenTeam.removePrefix(TC_DORFEN).trim()
            val has4er = liga.contains("(4er)") || always4er.contains(altersklasse)

            val displayName = buildString {
                append(altersklasse)
                if (teamSuffix.isNotEmpty()) {
                    append(" ")
                    append(teamSuffix)
                }
                if (has4er) {
                    append(" (4er)")
                }
            }

            val homeTeam = if (isHome) displayName else opponent
            val guestTeam = if (isHome) opponent else displayName

            Game(spieltermin, uhrzeit, liga, homeTeam, guestTeam, isHome)
        }

        val csvGames = games.map { game ->
            val prefix = if (game.isHome) "Heimspiel: " else "Auswärts: "
            CsvGame(
                "$prefix ${game.homeTeam} - ${game.guestTeam}",
                LocalDate.parse(game.day, localDatePattern),
                LocalTime.parse(game.time, localTimePattern),
                LocalDate.parse(game.day, localDatePattern),
                LocalTime.parse(game.time, localTimePattern).plusHours(5),
                false,
                "",
                ""
            )
        }

        return csvGames to games
    }
}
