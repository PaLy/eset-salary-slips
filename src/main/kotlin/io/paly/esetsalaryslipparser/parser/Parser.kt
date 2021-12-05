package io.paly.esetsalaryslipparser.parser

interface Parser<T> {
    fun parse(): T
}
