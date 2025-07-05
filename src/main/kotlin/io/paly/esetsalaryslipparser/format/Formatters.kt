package io.paly.esetsalaryslipparser.format

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*

fun skMonthToNumber(month: String): Int {
    return DateTimeFormatter
        .ofPattern("LLLL")
        .withLocale(Locale.of("sk"))
        .parse(month)
        .get(ChronoField.MONTH_OF_YEAR)
}

fun String.toUSDouble(): Double {
    return replace(',', '.').toDouble()
}

fun Double.format(decimalDigits: Int) = "%,.${decimalDigits}f".format(Locale.of("en"),this).replace(",", " ")

fun Int.format(digits: Int) = "%0${digits}d".format(this)

fun String.width(places: Int) = "%${places}s".format(this)
