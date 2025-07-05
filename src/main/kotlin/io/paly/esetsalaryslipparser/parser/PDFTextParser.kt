package io.paly.esetsalaryslipparser.parser

import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream

class PDFTextParser: Parser<String> {
    private var file: File? = null
    private var inputStream: InputStream? = null
    private var password: String? = null

    private constructor()

    constructor(inputStream: InputStream): this() {
        this.inputStream = inputStream
    }

    constructor(file: File, password: String): this() {
        this.file = file
        this.password = password
    }

    override fun parse(): String {
        return if (password != null) {
            Loader.loadPDF(file, password)
        } else {
            Loader.loadPDF(RandomAccessReadBuffer(inputStream))
        }
            .let(this::getText)
    }

    private fun getText(pdf: PDDocument): String {
        val pdfTextStripper = PDFTextStripper()
        pdfTextStripper.sortByPosition = true
        return pdfTextStripper.getText(pdf)
    }
}