package io.paly.esetsalaryslipparser

import io.paly.esetsalaryslipparser.args.getDebug
import io.paly.esetsalaryslipparser.args.getPassword
import io.paly.esetsalaryslipparser.args.getSlipsDirectory
import io.paly.esetsalaryslipparser.parser.WageIncomeParser
import io.paly.esetsalaryslipparser.parser.compareByDateTo
import io.paly.esetsalaryslipparser.print.pdf.PdfPrinter
import io.paly.esetsalaryslipparser.statistics.IncomeStatistics

class Main {
    companion object {
        var debug: Boolean = false

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                debug = getDebug(args)
                val password = getPassword(args)

                WageIncomeParser(getSlipsDirectory(args), password)
                    .parse()
                    .sortedWith { a, b -> a.compareByDateTo(b) }
                    .toList()
                    .let { incomes ->
                        val pdfPrinter = PdfPrinter()
                        IncomeStatistics(incomes).print(pdfPrinter)
                        val outputFileName = "income-statistics.pdf"
                        pdfPrinter.save(outputFileName, password)
                        println()
                        println("$outputFileName created. Use your password to open.")
                    }
            } catch (e: Exception) {
                System.err.println()
                System.err.println(e.message)
                if (!debug) {
                    System.err.println()
                    System.err.println("For debugging, run with --debug. (Sensitive) PDF content may be outputted to console.")
                }
            }
        }
    }
}