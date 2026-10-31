package io.paly.esetsalaryslipparser.print

interface Printer {
    var headerLines: List<String>
    fun println()
    fun println(line: String)
    fun isAtEndOfPage(): Boolean
    fun printBarChart(
        title: String,
        labels: List<String>,
        values: List<Double>,
        valueSuffix: String = "",
        barLegend: String? = null,
        lines: List<ChartLine> = emptyList(),
    )
}

/** Line series drawn over a bar chart. A null value leaves a gap in the line. */
data class ChartLine(
    val legend: String,
    val values: List<Double?>,
    val color: FloatArray,
    /** If true, the line uses the secondary (right) y-axis, labelled in the first such line's color; otherwise the bars' (left) y-axis. */
    val secondaryAxis: Boolean = false,
    val decimals: Int = 0,
    val valueSuffix: String = "",
    val labelBelow: Boolean = false,
    val dashed: Boolean = false,
)
