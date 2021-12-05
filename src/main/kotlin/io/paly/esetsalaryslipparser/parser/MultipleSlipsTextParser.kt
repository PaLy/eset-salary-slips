package io.paly.esetsalaryslipparser.parser

import java.io.File

class MultipleSlipsTextParser(private val directory: String, private val password: String) : Parser<Sequence<Pair<String, String>>> {

    override fun parse(): Sequence<Pair<String, String>> {
        val pdfFiles = getFiles("pdf")
        val pdfParsers = pdfFiles.map { PDFTextParser(it, password) }

        val zipFiles = getFiles("zip")
        val zipParsers = zipFiles.map { ZipTextParser(it, password) }

        return (pdfFiles + zipFiles).map { it.name }
            .zip(
                (pdfParsers + zipParsers).map { it.parse() }
            )
    }

    private fun getFiles(extension: String): Sequence<File> {
        val files = File(directory).listFiles()
        if (files != null) {
            return files
                .partition { it.name.endsWith(".$extension") }
                .let { (files) -> files.asSequence() }
        } else {
            throw RuntimeException("Directory $directory does not exist or an I/O error occurred.")
        }
    }
}