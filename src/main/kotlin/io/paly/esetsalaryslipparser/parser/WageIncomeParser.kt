package io.paly.esetsalaryslipparser.parser

import io.paly.esetsalaryslipparser.args.getPassword
import io.paly.esetsalaryslipparser.args.getSlipsDirectory
import io.paly.esetsalaryslipparser.format.skMonthToNumber
import io.paly.esetsalaryslipparser.format.toUSDouble
import io.paly.esetsalaryslipparser.regex.*
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
            "VYÚČTOVANIE MIEZD ZA MESIAC $INT/($INT)|($INT|$INT_WITH_SPACES) Stredisko".toRegex()
        )
        val year = oldYear.ifEmpty { newYear.replace(" ","") }

        val (oldMonth, newMonth) = text.findRegex(
            "VYÚČTOVANIE MIEZD ZA MESIAC ($INT)/|($NAME) *($INT|$INT_WITH_SPACES) Stredisko".toRegex()
        )
        val month = oldMonth.ifEmpty {
            skMonthToNumber(newMonth).toString()
        }

        val (cistaMzda) = text.findRegex("(?:ČISTÁ MZDA|Čistá mzda) *($DOUBLE)".toRegex())
        val (vyplata) = text.findRegex("(?:DOPLATOK MZDY NA ÚČET:|DOPLATOK MZDY {2}NA ÚČET:|Čiastka k výplate) *($DOUBLE)".toRegex())

        return WageIncome(
            year.toInt(),
            month.toInt(),
            cistaMzda.toUSDouble(),
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
