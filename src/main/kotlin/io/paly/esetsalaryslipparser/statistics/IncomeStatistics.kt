package io.paly.esetsalaryslipparser.statistics

import io.paly.esetsalaryslipparser.format.format
import io.paly.esetsalaryslipparser.format.width
import io.paly.esetsalaryslipparser.inflation.InflationData
import io.paly.esetsalaryslipparser.parser.WageIncome
import io.paly.esetsalaryslipparser.print.ChartLine
import io.paly.esetsalaryslipparser.print.Printer
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class IncomeStatistics(
    private val sortedIncomes: List<WageIncome>,
    private val inflation: InflationData = InflationData(),
) {
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

    companion object {
        /** Yearly salary growth above inflation expected from a good performer (typically 2-4 %). */
        const val GOOD_PERFORMER_REAL_GROWTH = 0.03

        /** Yearly salary growth above inflation expected from a high performer (typically 4-7 %). */
        const val HIGH_PERFORMER_REAL_GROWTH = 0.055
    }

    data class YearSummary(
        val year: Int,
        val movingAvgAnnualIncrease: Double,
        val movingAnnualAvg: Double,
        val inflation: Double?,
        /** Moving annual avg. in prices of the first year (with known prices) in the summaries. */
        val realMovingAnnualAvg: Double?,
        /** Moving annual avg. of the first year (with known prices) growing yearly by inflation + [GOOD_PERFORMER_REAL_GROWTH]. */
        val expectedMovingAnnualAvg: Double?,
        /** Same as [expectedMovingAnnualAvg], but growing by inflation + [HIGH_PERFORMER_REAL_GROWTH]. */
        val highPerformerMovingAnnualAvg: Double?,
    )

    /** For each year (except the first), values at the year's last month in the data. */
    fun yearlySummaries(): List<YearSummary> {
        val movingAvgAnnualIncrease = movingAvgAnnualIncrease()
        val movingAnnualAvg = movingAvg(12)

        val lastIndexByYear = sortedIncomes.indices
            .groupBy { sortedIncomes[it].year }
            .mapValues { (_, indices) -> indices.last() }
            .filterValues { lastIndex -> lastIndex >= 12 }
        val lastMonthByYear = lastIndexByYear.mapValues { (_, lastIndex) ->
            sortedIncomes[lastIndex].let { YearMonth.of(it.year, it.month) }
        }
        val priceLevelByYear = lastMonthByYear.mapValues { (_, month) -> movingAnnualPriceLevel(month) }
        val baseYear = priceLevelByYear.entries.firstOrNull { it.value != null }?.key
        val basePriceLevel = baseYear?.let { priceLevelByYear[it] }
        val baseIncome = baseYear?.let { movingAnnualAvg[lastIndexByYear.getValue(it)] }

        return lastIndexByYear.map { (year, lastIndex) ->
            val priceLevel = priceLevelByYear[year]
            fun expected(realGrowth: Double) =
                if (priceLevel != null && basePriceLevel != null && baseIncome != null) {
                    baseIncome * priceLevel / basePriceLevel * (1 + realGrowth).pow(year - baseYear)
                } else {
                    null
                }
            YearSummary(
                year = year,
                movingAvgAnnualIncrease = movingAvgAnnualIncrease[lastIndex],
                movingAnnualAvg = movingAnnualAvg[lastIndex],
                inflation = latestInYear(inflation.rate, lastMonthByYear.getValue(year)),
                realMovingAnnualAvg = if (priceLevel != null && basePriceLevel != null) {
                    movingAnnualAvg[lastIndex] * basePriceLevel / priceLevel
                } else {
                    null
                },
                expectedMovingAnnualAvg = expected(GOOD_PERFORMER_REAL_GROWTH),
                highPerformerMovingAnnualAvg = expected(HIGH_PERFORMER_REAL_GROWTH),
            )
        }
    }

    /**
     * Average price index of the 12 months ending at the given month (matching the moving annual avg. of income),
     * or ending at the latest earlier month of the same year if the given month is not yet published.
     */
    private fun movingAnnualPriceLevel(month: YearMonth): Double? {
        val lastPublished = generateSequence(month) { it.minusMonths(1) }
            .takeWhile { it.year == month.year }
            .firstOrNull { inflation.priceIndex.containsKey(it) }
            ?: return null
        val window = (0L until 12L).map { inflation.priceIndex[lastPublished.minusMonths(it)] }
        return if (window.all { it != null }) window.sumOf { it!! } / 12 else null
    }

    /** Value at the given month, or at the latest earlier month of the same year if not yet published. */
    private fun latestInYear(values: Map<YearMonth, Double>, month: YearMonth): Double? =
        generateSequence(month) { it.minusMonths(1) }
            .takeWhile { it.year == month.year }
            .firstNotNullOfOrNull { values[it] }

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

                if (income.month == 1 && !printer.isAtEndOfPage()) {
                    printer.println(dividingLine)
                }
                printer.println(
                    "${income.year - 2000}/${income.month.format(2)}${income.income.formatIncome()}      ${movingAvgAnnualIncrease.formatIncrease()} %       ${movingAnnualTotal.formatIncome()}    ${movingAnnualAvg.formatIncome()}   ${movingSemiAnnualAvg.formatIncome()}    ${(96 * movingAnnualAvg).formatIncome()}    ${(96 * movingSemiAnnualAvg).formatIncome()}"
                )
            }

        printer.println()
        val totalIncome = sortedIncomes.sumOf { it.income }
        printer.println("Total income: ${totalIncome.formatIncome()}")

        val summaries = yearlySummaries()
        if (summaries.isNotEmpty()) {
            val lines = mutableListOf(
                ChartLine(
                    legend = "Moving annual avg.",
                    values = summaries.map { it.movingAnnualAvg },
                    color = floatArrayOf(0.20f, 0.40f, 0.80f),
                    secondaryAxis = true,
                ),
            )
            val baseYear = summaries.firstOrNull { it.realMovingAnnualAvg != null }?.year
            if (baseYear != null) {
                lines += ChartLine(
                    legend = "Moving annual avg. in $baseYear prices",
                    values = summaries.map { it.realMovingAnnualAvg },
                    color = floatArrayOf(0.20f, 0.40f, 0.80f),
                    secondaryAxis = true,
                    dashed = true,
                    labelBelow = true,
                )
            }
            if (baseYear != null) {
                lines += ChartLine(
                    legend = "Good performer (inflation + ${(100 * GOOD_PERFORMER_REAL_GROWTH).format(0)} % yearly)",
                    values = summaries.map { it.expectedMovingAnnualAvg },
                    color = floatArrayOf(0.55f, 0.25f, 0.65f),
                    secondaryAxis = true,
                    dashed = true,
                )
            }
            if (baseYear != null) {
                lines += ChartLine(
                    legend = "High performer (inflation + ${(100 * HIGH_PERFORMER_REAL_GROWTH).format(1)} % yearly)",
                    values = summaries.map { it.highPerformerMovingAnnualAvg },
                    color = floatArrayOf(0.10f, 0.60f, 0.60f),
                    secondaryAxis = true,
                    dashed = true,
                )
            }
            if (summaries.any { it.inflation != null }) {
                lines += ChartLine(
                    legend = "Slovakia inflation (HICP, 12-month avg.)",
                    values = summaries.map { it.inflation },
                    color = floatArrayOf(0.95f, 0.55f, 0.10f),
                    decimals = 1,
                    valueSuffix = " %",
                    labelBelow = true,
                )
            }
            printer.printBarChart(
                title = "Yearly income (values at the last month of each year)",
                labels = summaries.map { it.year.toString() },
                values = summaries.map { it.movingAvgAnnualIncrease },
                valueSuffix = " %",
                barLegend = "Moving annual increase",
                lines = lines,
            )
        }
    }
}

fun Double.formatIncome() = this.format(2).width(10)
fun Double.formatIncrease() = this.format(2).width(5)