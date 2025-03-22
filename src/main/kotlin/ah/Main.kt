package ah

import java.io.File
import kotlin.system.exitProcess

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val inputFile = checkFileExists(args)
        val (csvGames, games) = BTVPlanExtractor.parseGamesBtv(inputFile)

        println("=====================================")
        println("Home teams:")
        val homeTeams = games.filter {  it.isHome }.map { it.homeTeam }.toSet().toList().sortedBy { it }
        homeTeams.forEach { team -> println(team) }
        println("=====================================")

        val outputFile = File(inputFile.parent, inputFile.nameWithoutExtension + ".csv")
        writeCsv(outputFile, CsvGoogleCalendar(matches = csvGames))
    }

    private fun checkFileExists(args: Array<String>): File {
        if (args.isEmpty()) {
            println("Error: Please provide a PDF file path as an argument.")
            println("Usage: java -jar btv-extract.jar <pdf-file-path>")
            exitProcess(1)
        }

        val inputFilePath = args[0]
        val inputFile = File(inputFilePath)

        if (!inputFile.exists()) {
            println("Error: The file '$inputFilePath' does not exist.")
            exitProcess(2)
        }

        if (!inputFile.isFile || !inputFile.canRead()) {
            println("Error: '$inputFilePath' is not a readable file.")
            exitProcess(3)
        }

        return inputFile
    }

    private fun writeCsv(file: File, calendar: CsvGoogleCalendar) {
        file.printWriter().use { out ->
            out.println(calendar.header.joinToString(calendar.separator))
            calendar.matches.forEach {
                out.println(it.toCsv(calendar.separator))
            }
        }
    }

}
