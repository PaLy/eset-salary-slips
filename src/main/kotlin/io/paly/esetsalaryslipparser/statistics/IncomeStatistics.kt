package io.paly.esetsalaryslipparser.statistics

import io.paly.esetsalaryslipparser.format.format
import io.paly.esetsalaryslipparser.format.width
import io.paly.esetsalaryslipparser.parser.WageIncome
import io.paly.esetsalaryslipparser.print.Printer
import kotlin.math.max
import kotlin.math.min

class IncomeStatistics(private val sortedIncomes: List<WageIncome>) {
    fun movingAvg(kMonths: Int): List<Double> {
        val movingSum = movingSum(kMonths)

        return movingSum.mapIndexed { index, sum ->
            val monthsCount = min(kMonths, index + 1)
            sum / monthsCount.toDouble()
        }
    }

    fun movingSum(kMonths: Int): List<Double> {
        return List(sortedIncomes.size) { index ->
            val firstMonthIndex = max(index - kMonths + 1, 0)
            val sum = sortedIncomes.subList(firstMonthIndex, index + 1).sumOf { it.income }
            sum
        }
    }

    fun movingAvgAnnualIncrease(): List<Double> {
        val movingAvg = movingAvg(12)

        return List(sortedIncomes.size) { index ->
            if (index < 12) {
                0.0
            } else {
                100 * movingAvg[index] / movingAvg[index - 12] - 100
            }
        }
    }

    fun print(printer: Printer) {
        val movingAnnualAvg = movingAvg(12)
        val movingSemiAnnualAvg = movingAvg(6)
        val movingAvgAnnualIncrease = movingAvgAnnualIncrease()
        val movingAnnualTotal = movingSum(12)
        val movingSemiAnnualTotal = movingSum(6)

        val header =
            "Month   Income       Moving          Moving        Moving     Moving semi-      96x       96x semi- \n" +
            "                 annual increase  annual total   annual avg.   annual avg.  annual avg.  annual avg."
        val dividingLine = "-----------------------------------------------------------------------------------------------------"
        printer.headerLines = "$header\n$dividingLine".split('\n')

        sortedIncomes
            .forEachIndexed { index, income ->
                val movingAnnualAvg = movingAnnualAvg[index]
                val movingAvgAnnualIncrease = movingAvgAnnualIncrease[index]
                val movingAnnualTotal = movingAnnualTotal[index]
                val movingSemiAnnualAvg = movingSemiAnnualAvg[index]

                if (income.month == 1) {
                    printer.println(dividingLine)
                }
                printer.println(
                    "${income.year - 2000}/${income.month.format(2)}${income.income.formatIncome()}      ${movingAvgAnnualIncrease.formatIncrease()} %       ${movingAnnualTotal.formatIncome()}    ${movingAnnualAvg.formatIncome()}   ${movingSemiAnnualAvg.formatIncome()}    ${(96 * movingAnnualAvg).formatIncome()}    ${(96 * movingSemiAnnualAvg).formatIncome()}"
                )
            }

        printer.println()
        val totalIncome = sortedIncomes.sumOf { it.income }
        printer.println("Total income: ${totalIncome.formatIncome()}")
    }
}

fun Double.formatIncome() = this.format(2).width(10).replace(",", " ")
fun Double.formatIncrease() = this.format(2).width(5)