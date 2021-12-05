package io.paly.esetsalaryslipparser.print.pdf

import io.paly.esetsalaryslipparser.print.Printer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font


class PdfPrinter: Printer {
    private val doc = PDDocument()
    private var linesWritten = 0
    private var contentStream: PDPageContentStream? = null

    override fun println() {
        addNewPageIfNeeded()
        contentStream!!.newLine()
        linesWritten++
    }

    private fun addNewPageIfNeeded() {
        if (linesWritten == 0 || linesWritten == linesPerPage) {
            linesWritten = 0
            contentStream?.endText()
            contentStream?.close()

            val page = PDPage()
            doc.addPage(page)
            contentStream =
                PDPageContentStream(doc, page)
                    .apply {
                        setFont(PDType1Font.COURIER, 12f)
                        setLeading(16f)
                    }
            contentStream!!.beginText()
            contentStream!!.newLineAtOffset(50f, 750f)
        }
    }

    override fun println(line: String) {
        addNewPageIfNeeded()
        contentStream!!.showText(line)
        contentStream!!.newLine()
        linesWritten++
    }

    fun save(filename: String, password: String) {
        contentStream?.endText()
        contentStream?.close()

        val ap = AccessPermission().apply {
            setCanPrint(false)
        }
        val spp = StandardProtectionPolicy(password, password, ap).apply {
            encryptionKeyLength = 256
        }
        doc.protect(spp)
        doc.save(filename)
        doc.close()
    }

    companion object {
        private const val linesPerPage = 45
    }
}

fun main() {
    with(PdfPrinter()) {
        println("hello")
        println("world")
        save("test.pdf", "123")
    }
}