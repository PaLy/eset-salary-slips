package io.paly.esetsalaryslipparser.parser

import io.paly.esetsalaryslipparser.args.getPassword
import io.paly.esetsalaryslipparser.args.getSlipsDirectory
import io.paly.esetsalaryslipparser.format. skMonthToNumber
import io.paly.esetsalaryslipparser.format.toUSDouble
import io.paly.esetsalaryslipparser.regex.DOUBLE
import io.paly.esetsalaryslipparser.regex.INT
import io.paly.esetsalaryslipparser.regex.NAME
import io.paly.esetsalaryslipparser.regex.findRegex
import java.lang.RuntimeException

class WageIncomeParser(private val directory: String, private val password: String) : Parser<Sequence<WageIncome>> {

    override fun parse(): Sequence<WageIncome> {
        return MultipleSlipsTextParser(directory, password)
            .parse()
            .map { (filename, text) ->
                try {
                    parseWageIncome(text)
                } catch (e: RuntimeException) {
                    throw RuntimeException("In file $filename:\n${e.message}")
                }
            }
    }

    private fun parseWageIncome(text: String): WageIncome {
        val (oldYear, newYear) = text.findRegex(
            "VYÚČTOVANIE MIEZD ZA MESIAC $INT/($INT)|($INT) Stredisko".toRegex()
        )
        val year = oldYear.ifEmpty { newYear }

        val (oldMonth, newMonth) = text.findRegex(
            "VYÚČTOVANIE MIEZD ZA MESIAC ($INT)/|($NAME) *$INT Stredisko".toRegex()
        )
        val month = oldMonth.ifEmpty {
            skMonthToNumber(newMonth).toString()
        }

        val (cistaMzda) = text.findRegex("(?:ČISTÁ MZDA|Čistá mzda) *($DOUBLE)".toRegex())
        // TODO looks like old slips don't have 'cisty prijem'
        val (cistyPrijem) = text.findRegex("(?:ČISTÝ PRÍJEM|Čiastka k výplate) *($DOUBLE)".toRegex())
        val (vyplata) = text.findRegex("(?:DOPLATOK MZDY NA ÚČET:|Čiastka k výplate) *($DOUBLE)".toRegex())

        return WageIncome(
            year.toInt(),
            month.toInt(),
            cistaMzda.toUSDouble(),
            cistyPrijem.toUSDouble(),
            vyplata.toUSDouble()
        )
    }
}

fun main(args: Array<String>) {
    WageIncomeParser(getSlipsDirectory(args), getPassword(args))
        .parse()
        .sortedWith { a, b -> a.compareByDateTo(b)}
        .forEach(::println)
}

data class WageIncome(
    val year: Int,
    val month: Int,
    val cistaMzda: Double,
    val cistyPrijem: Double,
    val income: Double,
)

fun WageIncome.compareByDateTo(other: WageIncome): Int {
    return year.compareTo(other.year)
        .let {
            if (it == 0) {
                month.compareTo(other.month)
            } else {
                it
            }
        }
}
