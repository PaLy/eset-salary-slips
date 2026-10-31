package io.paly.esetsalaryslipparser.inflation

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.YearMonth
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Slovakia HICP data by month. */
data class InflationData(
    /** Moving 12 months average rate of change (%). */
    val rate: Map<YearMonth, Double> = emptyMap(),
    /** Price index (2015 = 100). */
    val priceIndex: Map<YearMonth, Double> = emptyMap(),
) {
    fun isEmpty() = rate.isEmpty() && priceIndex.isEmpty()
}

/**
 * Slovakia HICP inflation, downloaded from Eurostat (dataset prc_hicp_minr) and cached locally.
 * When offline, the newer of the local cache and the bundled slovak-inflation.csv is used.
 */
object SlovakInflation {
    private const val EUROSTAT_URL =
        "https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_minr" +
            "?geo=SK&coicop18=TOTAL&lang=en&unit="
    private const val RATE_UNIT = "RCH_MV12MAVR"
    private const val INDEX_UNIT = "I15"
    private const val BUNDLED_RESOURCE = "/slovak-inflation.csv"
    private val cacheFile: Path = Path.of(System.getProperty("user.home"), ".eset-salary-slips", "slovak-inflation.csv")
    private const val CSV_HEADER =
        "# Slovakia HICP, Eurostat dataset prc_hicp_minr\n" +
            "# month,moving 12 months average rate of change (%),price index (2015 = 100)\n"

    fun load(): InflationData {
        try {
            val downloaded = download()
            if (!downloaded.isEmpty()) {
                saveCache(downloaded)
                return downloaded
            }
            System.err.println("Eurostat returned no inflation data, using local data.")
        } catch (e: Exception) {
            System.err.println("Cannot download inflation data from Eurostat (${e.message ?: e.javaClass.simpleName}), using local data.")
        }
        return listOf(loadCache(), loadBundled()).maxWith(
            compareBy<InflationData> { it.rate.keys.maxOrNull() ?: YearMonth.of(0, 1) }
                .thenBy { it.priceIndex.keys.maxOrNull() ?: YearMonth.of(0, 1) }
        )
    }

    private fun saveCache(data: InflationData) {
        try {
            Files.createDirectories(cacheFile.parent)
            val tempFile = Files.createTempFile(cacheFile.parent, "slovak-inflation", ".tmp")
            try {
                tempFile.writeText(toCsv(data))
                Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally {
                Files.deleteIfExists(tempFile)
            }
        } catch (e: Exception) {
            System.err.println("Cannot save inflation data cache to $cacheFile (${e.message ?: e.javaClass.simpleName}).")
        }
    }

    private fun loadCache(): InflationData =
        try {
            if (cacheFile.exists()) parseCsv(cacheFile.readText()) else InflationData()
        } catch (e: Exception) {
            System.err.println("Cannot read inflation data cache $cacheFile (${e.message ?: e.javaClass.simpleName}).")
            InflationData()
        }

    private fun download(): InflationData {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        fun downloadUnit(unit: String): Map<YearMonth, Double> {
            val request = HttpRequest.newBuilder(URI(EUROSTAT_URL + unit))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == 200) { "HTTP ${response.statusCode()}" }
            return parseEurostatJson(response.body())
        }

        return InflationData(rate = downloadUnit(RATE_UNIT), priceIndex = downloadUnit(INDEX_UNIT))
    }

    /** Parses Eurostat JSON-stat 2.0 response with a single series varying only by time. */
    internal fun parseEurostatJson(json: String): Map<YearMonth, Double> {
        val values = Regex(""""value"\s*:\s*\{([^}]*)}""").find(json)?.groupValues?.get(1) ?: return emptyMap()
        val timeIndex = Regex(""""time"\s*:\s*\{.*?"index"\s*:\s*\{([^}]*)}""", RegexOption.DOT_MATCHES_ALL)
            .find(json)?.groupValues?.get(1) ?: return emptyMap()

        val positionToMonth = Regex(""""(\d{4}-\d{2})"\s*:\s*(\d+)""").findAll(timeIndex)
            .associate { it.groupValues[2].toInt() to YearMonth.parse(it.groupValues[1]) }

        return Regex(""""(\d+)"\s*:\s*(-?[\d.]+)""").findAll(values)
            .mapNotNull { match ->
                positionToMonth[match.groupValues[1].toInt()]?.let { it to match.groupValues[2].toDouble() }
            }
            .toMap()
            .toSortedMap()
    }

    private fun loadBundled(): InflationData =
        SlovakInflation::class.java.getResource(BUNDLED_RESOURCE)?.readText()?.let { parseCsv(it) } ?: InflationData()

    internal fun toCsv(data: InflationData): String {
        val months = (data.rate.keys + data.priceIndex.keys).sorted()
        return CSV_HEADER + months.joinToString("") { month ->
            "$month,${data.rate[month] ?: ""},${data.priceIndex[month] ?: ""}\n"
        }
    }

    internal fun parseCsv(text: String): InflationData {
        val rows = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val fields = line.split(',')
                Triple(YearMonth.parse(fields[0]), fields.getOrNull(1)?.toDoubleOrNull(), fields.getOrNull(2)?.toDoubleOrNull())
            }
            .toList()
        return InflationData(
            rate = rows.mapNotNull { (month, rate, _) -> rate?.let { month to it } }.toMap().toSortedMap(),
            priceIndex = rows.mapNotNull { (month, _, index) -> index?.let { month to it } }.toMap().toSortedMap(),
        )
    }
}
