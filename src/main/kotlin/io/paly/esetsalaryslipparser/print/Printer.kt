package io.paly.esetsalaryslipparser.print

interface Printer {
    var headerLines: List<String>
    fun println()
    fun println(line: String)
}