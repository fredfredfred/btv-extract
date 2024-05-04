package ah

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PDFProcessor {

    private val tesseract = Tesseract()
    private val textStripper = PDFTextStripper()
    private val newLine = System.lineSeparator()

    init {
        System.setProperty("jna.library.path", "/opt/homebrew/Cellar/tesseract/5.3.1_1/lib")
        tesseract.setDatapath("/opt/homebrew/share/tessdata")
        tesseract.setLanguage("deu")
        tesseract.setOcrEngineMode(1)
    }

    fun processPdf(sourcePdfPath: String): String {
        val document = Loader.loadPDF(File(sourcePdfPath))

        document.use {
            val pages = it.pages
            val pageStrings = pages.map { page ->
                val pageText = extractPageText(page)
                val imageTexts = extractAndOcrImages(page)
                val result = mutableListOf(pageText)
                result.addAll(imageTexts)
                result.joinToString(newLine)
            }
            return pageStrings.joinToString(newLine + newLine)
        }
    }

    private fun extractPageText(page: PDPage): String {
        val document = PDDocument()
        document.addPage(page)
        val text = textStripper.getText(document)
        document.close()
        return text
    }

    private fun extractAndOcrImages(page: PDPage): List<String> {
        val pdResources = page.resources
        return pdResources.xObjectNames
            .mapNotNull { pdResources.getXObject(it) }
            .filterIsInstance<PDImageXObject>()
            .mapNotNull { image ->
                try {
                    tesseract.doOCR(image.image)
                } catch (e: TesseractException) {
                    e.printStackTrace()
                    null
                }
            }
    }
}

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
    val isHome: Boolean = homeDorfen.any { homeTeam.contains(it) },
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
    val games: List<CsvGame>,
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

val homeDorfen = setOf("Herren", "Damen", "Knaben", "Mädchen", "Dunlop", "Bambini", "Junioren", "Juniorinnen")
val stopWords = setOf("Spielort:", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.", "So.", "nu.Dokument")
val dorfenTeamOneLiner = setOf("Herren 70 (4er)", "Dunlop Kleinfeld U9 (4er)", "Knaben 15 (4er)", "Damen 40")
val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val localTimePattern = DateTimeFormatter.ofPattern("HH:mm")
val usTimePattern = DateTimeFormatter.ofPattern("h:mm a")

fun main() {
    parseGamesBtv()
}

fun parseGamesBtv() {
    val spielplan = PDFProcessor().processPdf("data/tc-dorfen-2024.pdf")

    val sectionMarker = "Termin Liga Heimmannschaft Gastmannschaft Bem. Erg."
    val sections = spielplan.split(sectionMarker)

    val timePattern = Regex("""\d{2}:\d{2}""")
    val datePattern = Regex("""\d{2}\.\d{2}\.\d{4}""")
    val leaguePattern = Regex("""(S|LL)\d{1}|RLSO""")
    val teamPattern = Regex("""([A-ZÄÖÜ][- .a-zA-Z_0-9()äöüßÄÖÜ]*)\n""")

    val games = sections.filter { it.isNotEmpty() }.flatMap { section ->
        val days = datePattern.findAll(section)

        if (days.count() == 0) {
            println("SKIP - No days found")
            return@flatMap emptyList<Game>()
        }

        val daySections = days.map { day ->
            val dayString = day.value
            val daySectionStartIndex = day.range.last + 1
            val nextDay = days.find { it.range.first > daySectionStartIndex }
            val nextDayStartIndex = nextDay?.range?.first ?: section.length

            val findLegende = section.indexOf("Legende")
            val endIndex =
                if (findLegende != -1 && findLegende < nextDayStartIndex && findLegende > daySectionStartIndex) {
                    findLegende
                } else {
                    nextDayStartIndex
                }
            val daySection = section.substring(daySectionStartIndex, endIndex)

            val cleanSection = cleanupDaySection(daySection)
            DaySection(dayString, cleanSection)
        }

        val games = daySections.flatMap { daySection ->
            val times = timePattern.findAll(daySection.section)
            val leagues = leaguePattern.findAll(daySection.section)

            if (times.count() == 0 || leagues.count() == 0) {
                println("SKIP - No times or leagues found for day ${daySection.day} in section ${daySection.section}")
                return@flatMap emptySequence<Game>()
            }

            val teams = teamPattern.findAll(daySection.section.substring(leagues.last().range.last + 1))

            if (teams.count() < 2) {
                println("SKIP - Teams less than 2: ${teams.count()} ${daySection.section}")
                return@flatMap emptySequence<Game>()
            }

            val homeTeams = teams.toList().subList(0, teams.count() / 2)
            val guestTeams = teams.toList().subList(teams.count() / 2, teams.count())

            if (times.count() != leagues.count() || times.count() != homeTeams.count() || times.count() != guestTeams.count()) {
                println("SKIP - Mismatch in counts times=${times.count()} leagues=${leagues.count()} home=${homeTeams.count()} guest=${guestTeams.count()} teams=${teams.count()}")
                println("home:" + homeTeams.joinToString("\n") { it.value.trim() })
                println("guest:" + guestTeams.joinToString { it.value.trim() })
                return@flatMap emptySequence<Game>()
            }

            times.mapIndexed { index, time ->
                val league = leagues.elementAt(index).value
                val home = homeTeams.elementAt(index).groups[1]?.value ?: "home team not found"
                val guest = guestTeams.elementAt(index).groups[1]?.value ?: "guest team not found"
                Game(daySection.day, time.value, league, home.trim(), guest.trim())
            }
        }

        games.toList()

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

    writeCsv(File("data/tc-dorfen-2024.csv"), CsvGoogleCalendar(games = csvGames))
}

fun writeCsv(file: File, calendar: CsvGoogleCalendar) {
    file.printWriter().use { out ->
        out.println(calendar.header.joinToString (calendar.separator))
        calendar.games.forEach {
            out.println(it.toCsv(calendar.separator))
        }
    }
}


fun cleanupDaySection(daySection: String): String {
    if (daySection.isBlank()) {
        return ""
    }
    val filtered = daySection.lines().filter {
        it.isNotBlank() && stopWords.none { stopWord -> it.startsWith(stopWord) }
    }.joinToString("\n", "", "\n")

    return if (filtered.lines().size <= 2) {
        filtered.lines().flatMap { line ->
            val foundDorfen = dorfenTeamOneLiner.find { line.contains(it) }
            if (foundDorfen != null) {
                val split = line.split(foundDorfen)
                listOf(split[0], foundDorfen, split[1])
            } else {
                listOf(line)
            }
        }.joinToString("\n", "", "\n")
    } else {
        filtered
    }

}
