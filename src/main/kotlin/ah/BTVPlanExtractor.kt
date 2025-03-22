package ah

import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val homeTeamStarters = setOf("Herren", "Damen", "Knaben", "Mädchen", "Dunlop", "Bambini", "Junioren", "Juniorinnen")

object BTVPlanExtractor {
    private val stopWords =
        setOf("Spielort:", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.", "So.", "nu.Dokument", "» ursprünglich", " HP ")
    private val localDatePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val localTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val timePattern = Regex("""\d{2}:\d{2}""")
    private val datePattern = Regex("""(?<!ursprünglich )\d{2}\.\d{2}\.\d{4}""")
    private val leaguePattern = Regex("""(S|LL)\d{1}|RLSO""")
    private val teamPattern = Regex("""([A-ZÄÖÜ][- .a-zA-Z_0-9()äöüßÄÖÜ/]*)\n""")
    private val longLeaguePattern = Regex("""(Regionalliga|Landesliga|Südliga)""")

    private fun parseHomeTeamNames(pdfText: String): Set<String> {
        val spielplanMarker = "Spielplan"
        val teamsSection = pdfText.split(spielplanMarker)[0]

        return teamsSection.lines()
            .filter { line ->homeTeamStarters.any { line.contains(it) } }
            .map {
                if (longLeaguePattern.containsMatchIn(it)) {
                    // one liner like 'Herren 70 (4er) Regionalliga Süd-Ost'
                    val teamNameEndIndex = longLeaguePattern.find(it)?.range?.first
                    return@map it.substring(0, teamNameEndIndex!!).trim()
                } else {
                    // team fills the whole line
                    return@map it.trim()
                }
            }.toSet()
    }

    fun parseGamesBtv(inputFile: File): Pair<List<CsvGame>, List<Game>> {
        val pdfText = PDFProcessor.processPdf(inputFile)
        val rawFile = File(inputFile.parent, inputFile.nameWithoutExtension + "-debug.txt")
        writeRaw(rawFile, pdfText)

        val homeTeamNames = parseHomeTeamNames(pdfText)

        val sectionMarker = "Termin Liga Heimmannschaft Gastmannschaft Bem. Erg."
        val sections = pdfText.split(sectionMarker)

        val games = sections.filter { it.isNotEmpty() }.flatMap sectionsMap@{ section ->
            val days = datePattern.findAll(section)

            if (days.count() == 0) {
                println("SKIP - No days found in section ${section.substring(0, 10)} ...")
                return@sectionsMap emptyList<Game>()
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

                val cleanSection = cleanupDaySection(daySection, homeTeamNames)
                DaySection(dayString, cleanSection)
            }

            val games = daySections.flatMap daySectionsMap@{ daySection ->
                val times = timePattern.findAll(daySection.section)
                val leagues = leaguePattern.findAll(daySection.section)

                if (times.count() == 0 || leagues.count() == 0) {
                    println("SKIP - No times or leagues found for day ${daySection.day} in day section ${daySection.section}")
                    return@daySectionsMap emptySequence<Game>()
                }

                val teams = teamPattern.findAll(daySection.section.substring(leagues.last().range.last + 1))

                if (teams.count() < 2) {
                    // check for one liner day section
                    println("SKIP - Teams less than 2: ${teams.count()} ${daySection.section}")
                    return@daySectionsMap emptySequence<Game>()
                }

                val homeTeams = teams.toList().subList(0, teams.count() / 2)
                val guestTeams = teams.toList().subList(teams.count() / 2, teams.count())

                if (times.count() != leagues.count() || times.count() != homeTeams.count() || times.count() != guestTeams.count()) {
                    println("SKIP - Mismatch in counts times=${times.count()} leagues=${leagues.count()} home=${homeTeams.count()} guest=${guestTeams.count()} teams=${teams.count()}")
                    println("home:" + homeTeams.joinToString("\n") { it.value.trim() })
                    println("guest:" + guestTeams.joinToString { it.value.trim() })
                    println("daySection: ${daySection.section}")
                    println("----------------")
                    return@daySectionsMap emptySequence<Game>()
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

        return csvGames to games
    }

    private fun writeRaw(file: File, string: String) {
        file.printWriter().use { out ->
            out.println(string)
        }
    }


    private fun cleanupDaySection(daySection: String, homeTeamNamesSet: Set<String>): String {
        if (daySection.isBlank()) {
            return ""
        }
        val filtered = daySection.lines().filter {
            it.isNotBlank() && stopWords.none { stopWord -> it.startsWith(stopWord) }
        }.joinToString("\n", "", "\n")

        val homeTeamNames = homeTeamNamesSet.toList().sortedByDescending { it.length }
        return if (filtered.lines().size <= 2) {
            filtered.lines().flatMap { line ->
                val foundHomeTeam = homeTeamNames.find { line.contains(it) }
                if (foundHomeTeam != null) {
                    val split = line.split(foundHomeTeam)
                    listOf(split[0], foundHomeTeam, split[1])
                } else {
                    listOf(line)
                }
            }.joinToString("\n", "", "\n")
        } else {
            filtered
        }

    }

}
