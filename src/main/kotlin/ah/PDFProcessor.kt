package ah

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File


object PDFProcessor {
    private val textStripper = PDFTextStripper()
    private val newLine = System.lineSeparator()

    fun processPdf(sourceFile: File): String {
        val document = Loader.loadPDF(sourceFile)

        document.use {
            val pages = it.pages
            val pageStrings = pages.map { page ->
                val pageText = extractPageText(page)
                val result = mutableListOf(pageText)
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
}

