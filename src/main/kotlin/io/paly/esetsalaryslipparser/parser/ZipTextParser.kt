package io.paly.esetsalaryslipparser.parser

import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.File
import java.io.FileInputStream

class ZipTextParser(private val file: File, private val password: String): Parser<String> {

    override fun parse(): String {
        return ZipInputStream(FileInputStream(file), password.toCharArray()).use { zipInputStream ->
            zipInputStream.nextEntry
            PDFTextParser(zipInputStream).parse()
        }
    }
}