package io.paly.esetsalaryslipparser.print.pdf

import io.paly.esetsalaryslipparser.format.format
import io.paly.esetsalaryslipparser.print.ChartLine
import io.paly.esetsalaryslipparser.print.Printer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt


class PdfPrinter: Printer {
    private val doc = PDDocument()
    private var linesWritten = 0
    private var contentStream: PDPageContentStream? = null
    override var headerLines = emptyList<String>()

    override fun isAtEndOfPage() = linesWritten == linesPerPage

    private fun addNewPageIfNeeded() {
        if (linesWritten == 0 || linesWritten == linesPerPage) {
            linesWritten = 0
            contentStream?.endText()
            contentStream?.close()

            val page = PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
            doc.addPage(page)
            contentStream =
                PDPageContentStream(doc, page)
                    .apply {
                        setFont(PDType1Font(Standard14Fonts.FontName.COURIER), 12f)
                        setLeading(16f)
                    }
            contentStream!!.beginText()
            contentStream!!.newLineAtOffset(50f, 550f)
            printHeader()
        }
    }

    private fun printHeader() {
        if (headerLines.isNotEmpty()) {
            headerLines.forEach {
                contentStream!!.showText(it)
                contentStream!!.newLine()
                linesWritten++
            }
        }
    }

    override fun println() {
        addNewPageIfNeeded()
        contentStream!!.newLine()
        linesWritten++
    }

    override fun println(line: String) {
        addNewPageIfNeeded()
        contentStream!!.showText(line)
        contentStream!!.newLine()
        linesWritten++
    }

    override fun printBarChart(
        title: String,
        labels: List<String>,
        values: List<Double>,
        valueSuffix: String,
        barLegend: String?,
        lines: List<ChartLine>,
    ) {
        require(labels.size == values.size) { "labels and values must have the same size" }
        require(lines.all { it.values.size == values.size }) { "line values and values must have the same size" }
        if (values.isEmpty()) return

        contentStream?.endText()
        contentStream?.close()
        contentStream = null

        val pageSize = PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
        val page = PDPage(pageSize)
        doc.addPage(page)

        val regularFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val boldFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val fontSize = 10f
        val white = floatArrayOf(1f, 1f, 1f)
        val black = floatArrayOf(0f, 0f, 0f)
        val positiveBarColor = floatArrayOf(0.30f, 0.65f, 0.35f)
        val negativeBarColor = floatArrayOf(0.85f, 0.30f, 0.30f)

        val secondaryLine = lines.firstOrNull { it.secondaryAxis }
        val left = 90f
        val right = pageSize.width - if (secondaryLine != null) 90f else 50f
        val bottom = 80f
        val top = pageSize.height - 110f

        val primaryValues = values + lines.filter { !it.secondaryAxis }.flatMap { it.values.filterNotNull() }
        val (axisMin, axisMax, step) = niceAxis(min(0.0, primaryValues.min()), max(0.0, primaryValues.max()))
        val intervals = ((axisMax - axisMin) / step).roundToInt()
        fun yOf(value: Double) = (bottom + (value - axisMin) / (axisMax - axisMin) * (top - bottom)).toFloat()
        val zeroY = yOf(0.0)

        val secondaryAxis = lines.filter { it.secondaryAxis }.flatMap { it.values.filterNotNull() }.takeIf { it.isNotEmpty() }
            ?.let { alignedAxis(it.min(), it.max(), intervals) }
        fun secondaryYOf(value: Double): Float {
            val (secondaryMin, secondaryStep) = secondaryAxis!!
            return (bottom + (value - secondaryMin) / (secondaryStep * intervals) * (top - bottom)).toFloat()
        }
        fun lineYOf(line: ChartLine, value: Double) = if (line.secondaryAxis) secondaryYOf(value) else yOf(value)

        PDPageContentStream(doc, page).use { cs ->
            fun textWidth(font: PDType1Font, size: Float, s: String) = font.getStringWidth(s) / 1000f * size
            fun text(font: PDType1Font, size: Float, x: Float, y: Float, s: String, align: Float = 0.5f) {
                cs.beginText()
                cs.setFont(font, size)
                cs.newLineAtOffset(x - textWidth(font, size, s) * align, y)
                cs.showText(s)
                cs.endText()
            }
            fun fillColor(color: FloatArray) = cs.setNonStrokingColor(color[0], color[1], color[2])
            fun strokeColor(color: FloatArray) = cs.setStrokingColor(color[0], color[1], color[2])

            text(boldFont, 16f, pageSize.width / 2, pageSize.height - 45f, title)

            // grid lines with y-axis labels
            cs.setLineWidth(0.5f)
            for (i in 0..intervals) {
                val tick = axisMin + i * step
                val y = yOf(tick)
                cs.setStrokingColor(0.85f, 0.85f, 0.85f)
                cs.moveTo(left, y)
                cs.lineTo(right, y)
                cs.stroke()
                fillColor(black)
                text(regularFont, fontSize, left - 8f, y - 3f, "${tick.format(1)}$valueSuffix", 1f)
                if (secondaryLine != null && secondaryAxis != null) {
                    val (secondaryMin, secondaryStep) = secondaryAxis
                    fillColor(secondaryLine.color)
                    val tickLabel = "${(secondaryMin + i * secondaryStep).format(0)}${secondaryLine.valueSuffix}"
                    text(regularFont, fontSize, right + 8f, y - 3f, tickLabel, 0f)
                }
            }

            // bars
            val slot = (right - left) / values.size
            val barWidth = min(slot * 0.6f, 60f)
            fun centerXOf(i: Int) = left + slot * (i + 0.5f)
            fun barColor(value: Double) = if (value >= 0) positiveBarColor else negativeBarColor
            values.forEachIndexed { i, value ->
                val centerX = centerXOf(i)
                val valueY = yOf(value)
                fillColor(barColor(value))
                cs.addRect(centerX - barWidth / 2, min(zeroY, valueY), barWidth, abs(valueY - zeroY))
                cs.fill()
                fillColor(black)
                text(regularFont, fontSize, centerX, bottom - 18f, labels[i])
            }

            // axes
            strokeColor(black)
            cs.setLineWidth(1f)
            cs.moveTo(left, bottom)
            cs.lineTo(left, top)
            cs.stroke()
            if (secondaryLine != null) {
                cs.moveTo(right, bottom)
                cs.lineTo(right, top)
                cs.stroke()
            }
            cs.moveTo(left, zeroY)
            cs.lineTo(right, zeroY)
            cs.stroke()

            // line series
            lines.forEach { line ->
                strokeColor(line.color)
                cs.setLineWidth(2f)
                if (line.dashed) cs.setLineDashPattern(floatArrayOf(6f, 4f), 0f)
                var previousDefined = false
                line.values.forEachIndexed { i, value ->
                    if (value != null) {
                        if (previousDefined) cs.lineTo(centerXOf(i), lineYOf(line, value))
                        else cs.moveTo(centerXOf(i), lineYOf(line, value))
                    }
                    previousDefined = value != null
                }
                cs.stroke()
                cs.setLineDashPattern(floatArrayOf(), 0f)

                fillColor(line.color)
                line.values.forEachIndexed { i, value ->
                    if (value != null) {
                        cs.addRect(centerXOf(i) - 3f, lineYOf(line, value) - 3f, 6f, 6f)
                        cs.fill()
                    }
                }
            }

            // labels are drawn after the lines, with a background, so the lines never cross them
            val placedLabels = mutableListOf<PDRectangle>()
            fun labelBox(x: Float, y: Float, label: String, font: PDType1Font, size: Float): PDRectangle {
                val w = textWidth(font, size, label)
                return PDRectangle(x - w / 2 - 2f, y - 3f, w + 4f, size + 3f)
            }
            fun overlap(a: PDRectangle, b: PDRectangle): Float {
                val w = min(a.upperRightX, b.upperRightX) - max(a.lowerLeftX, b.lowerLeftX)
                val h = min(a.upperRightY, b.upperRightY) - max(a.lowerLeftY, b.lowerLeftY)
                return if (w > 0 && h > 0) w * h else 0f
            }
            fun labelWithBackground(
                x: Float, y: Float, label: String, font: PDType1Font, size: Float,
                background: FloatArray, foreground: FloatArray,
            ) {
                val box = labelBox(x, y, label, font, size)
                placedLabels += box
                fillColor(background)
                cs.addRect(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
                cs.fill()
                fillColor(foreground)
                text(font, size, x, y, label)
            }
            /** Draws the label at the first candidate y not overlapping already placed labels (or the least overlapping one). */
            fun placeLabel(
                x: Float, candidateYs: List<Float>, label: String, font: PDType1Font, size: Float,
                background: FloatArray, foreground: FloatArray,
            ) {
                val y = candidateYs.minBy { candidateY ->
                    val box = labelBox(x, candidateY, label, font, size)
                    placedLabels.sumOf { overlap(box, it).toDouble() }
                }
                labelWithBackground(x, y, label, font, size, background, foreground)
            }

            val barLabels = values.map { "${it.format(2)}$valueSuffix" }
            // one shared size for all bar labels, small enough for the widest label (incl. background padding) to fit in a bar
            val widestBarLabel = barLabels.maxOf { textWidth(boldFont, 1f, it) }
            val barLabelSize = min(fontSize, (barWidth - 6f) / widestBarLabel).coerceAtLeast(5f)
            values.forEachIndexed { i, value ->
                val valueY = yOf(value)
                val fitsInside = abs(valueY - zeroY) >= barLabelSize + 10f
                val labelY = when {
                    fitsInside && value >= 0 -> valueY - barLabelSize - 3f
                    fitsInside -> valueY + 6f
                    value >= 0 -> valueY + 4f
                    else -> valueY - barLabelSize - 2f
                }
                if (fitsInside) labelWithBackground(centerXOf(i), labelY, barLabels[i], boldFont, barLabelSize, barColor(value), white)
                else labelWithBackground(centerXOf(i), labelY, barLabels[i], boldFont, barLabelSize, white, black)
            }

            // skip labels equal to an already drawn label of another line at the same point on the same axis
            val drawnLineLabels = mutableSetOf<Triple<Int, Boolean, String>>()
            lines.forEach { line ->
                line.values.forEachIndexed { i, value ->
                    if (value != null) {
                        val label = "${value.format(line.decimals)}${line.valueSuffix}"
                        if (drawnLineLabels.add(Triple(i, line.secondaryAxis, label))) {
                            val y = lineYOf(line, value)
                            val above = listOf(y + 9f, y + 9f + fontSize + 5f)
                            val below = listOf(y - fontSize - 6f, y - 2 * fontSize - 11f)
                            val candidates = (if (line.labelBelow) below.zip(above) else above.zip(below))
                                .flatMap { it.toList() }
                                .filter { it >= bottom + 2f && it + fontSize <= top }
                                .ifEmpty { listOf(y + 9f) }
                            placeLabel(centerXOf(i), candidates, label, regularFont, fontSize, white, line.color)
                        }
                    }
                }
            }

            // legend, wrapped into rows
            var legendX = left
            var legendY = pageSize.height - 70f
            fun legendItem(label: String, drawSymbol: (x: Float, y: Float) -> Unit) {
                val width = 26f + textWidth(regularFont, fontSize, label)
                if (legendX > left && legendX + width > right) {
                    legendX = left
                    legendY -= 15f
                }
                drawSymbol(legendX, legendY)
                fillColor(black)
                text(regularFont, fontSize, legendX + 26f, legendY + 1f, label, 0f)
                legendX += width + 30f
            }
            if (barLegend != null) {
                legendItem(barLegend) { x, y ->
                    fillColor(positiveBarColor)
                    cs.addRect(x + 4f, y, 12f, 10f)
                    cs.fill()
                }
            }
            lines.forEach { line ->
                legendItem(line.legend) { x, y ->
                    strokeColor(line.color)
                    cs.setLineWidth(2f)
                    if (line.dashed) cs.setLineDashPattern(floatArrayOf(4f, 2f), 0f)
                    cs.moveTo(x, y + 5f)
                    cs.lineTo(x + 20f, y + 5f)
                    cs.stroke()
                    cs.setLineDashPattern(floatArrayOf(), 0f)
                    fillColor(line.color)
                    cs.addRect(x + 7f, y + 2f, 6f, 6f)
                    cs.fill()
                }
            }
        }

        linesWritten = 0
    }

    private fun niceSteps(rawStep: Double): Sequence<Double> {
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        return generateSequence(magnitude) { it * 10 }
            .flatMap { m -> listOf(1.0, 2.0, 2.5, 5.0).map { it * m } }
            .filter { it >= rawStep }
    }

    private fun niceAxis(minValue: Double, maxValue: Double): Triple<Double, Double, Double> {
        val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val step = niceSteps(range / 8).first()
        val axisMin = floor(minValue / step) * step
        var axisMax = ceil(maxValue / step) * step
        // leave room for value labels above the tallest bar
        if (axisMax - maxValue < step / 3) axisMax += step
        return Triple(axisMin, axisMax, step)
    }

    /** Axis (min, step) with exactly [intervals] intervals, so its ticks align with the primary axis grid lines. */
    private fun alignedAxis(minValue: Double, maxValue: Double, intervals: Int): Pair<Double, Double> {
        val range = (maxValue - minValue).takeIf { it > 0 } ?: max(abs(maxValue), 1.0)
        return niceSteps(range / intervals)
            .map { step -> floor(minValue / step) * step to step }
            // leave room for value labels above the highest point
            .first { (axisMin, step) -> axisMin + intervals * step - maxValue >= step / 3 }
    }

    fun save(filename: String, password: String) {
        contentStream?.endText()
        contentStream?.close()

        val ap = AccessPermission().apply {
            setCanPrint(false)
        }
        val spp = StandardProtectionPolicy(password, password, ap).apply {
            encryptionKeyLength = 256
        }
        doc.protect(spp)
        doc.save(filename)
        doc.close()
    }

    companion object {
        private const val linesPerPage = 33
    }
}

fun main() {
    with(PdfPrinter()) {
        println("hello")
        println("world")
        save("test.pdf", "123")
    }
}