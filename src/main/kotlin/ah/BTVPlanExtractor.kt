package ah

import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val stopWords = setOf("Spielort:", "Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.", "So.", "nu.Dokument", "» ursprünglich")
val dorfenTeamOneLiner = setOf("Herren 70 (4er)", "Dunlop Kleinfeld U9 (4er)", "Knaben 15 (4er)", "Damen 40")

val localDatePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val localTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val usTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

object BTVPlanExtractor {

    fun parseGamesBtv(inputFile: File): List<CsvGame> {
        val spielplan = PDFProcessor.processPdf(inputFile)
        val rawFile = File(inputFile.parent, inputFile.nameWithoutExtension + "-debug.txt")
        writeRaw(rawFile, spielplan)

        val sectionMarker = "Termin Liga Heimmannschaft Gastmannschaft Bem. Erg."
        val sections = spielplan.split(sectionMarker)

        val timePattern = Regex("""\d{2}:\d{2}""")
        val datePattern = Regex("""(?<!ursprünglich )\d{2}\.\d{2}\.\d{4}""")
        val leaguePattern = Regex("""(S|LL)\d{1}|RLSO""")
        val teamPattern = Regex("""([A-ZÄÖÜ][- .a-zA-Z_0-9()äöüßÄÖÜ/]*)\n""")

        val games = sections.filter { it.isNotEmpty() }.flatMap sectionsMap@ { section ->
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

                val cleanSection = cleanupDaySection(daySection)
                DaySection(dayString, cleanSection)
            }

            val games = daySections.flatMap daySectionsMap@ { daySection ->
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

        return csvGames
    }

    private fun writeRaw(file: File, string: String) {
        file.printWriter().use { out ->
            out.println(string)
        }
    }


    private fun cleanupDaySection(daySection: String): String {
        if (daySection.isBlank()) {
            return ""
        }
        val filtered = daySection.lines().filter {
            it.isNotBlank() && stopWords.none { stopWord -> it.startsWith(stopWord) }
        }.joinToString("\n", "", "\n")

        return if (filtered.lines().size <= 2) {
            filtered.lines().flatMap linesMap@ { line ->
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

}
