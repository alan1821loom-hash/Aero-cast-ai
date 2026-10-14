package com.example.ml

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sqrt

enum class ImputationStrategy(val label: String) {
    FORWARD_FILL("Forward-Fill (Time-Series)"),
    MEAN("Column Mean"),
    MEDIAN("Column Median")
}

data class PreprocessingAudit(
    val originalRowCount: Int,
    val cleanedRowCount: Int,
    val missingValuesImputed: Int,
    val duplicatesRemoved: Int,
    val selectedFeatures: List<String>,
    val targetVariable: String,
    val trainCount: Int,
    val testCount: Int,
    val chronologicalSortingApplied: Boolean,
    val scalingMethod: String = "StandardScaler (Fitted on Training Set Only)"
)

data class ParsedDataset(
    val headers: List<String>,
    val rows: List<List<String>>,
    val detectedAliases: Map<String, String>, // header -> standard alias
    val detectedTimestampColumn: String?,
    val detectedTargetCandidate: String
)

object Preprocessor {

    private val ALIAS_PATTERNS = mapOf(
        "PM2.5" to listOf("pm2.5", "pm25", "pm2_5", "fine_particles", "pm_25"),
        "PM10" to listOf("pm10", "pm_10", "coarse_particles"),
        "CO" to listOf("co", "carbon_monoxide", "co_ppm"),
        "CO2" to listOf("co2", "carbon_dioxide"),
        "NO2" to listOf("no2", "nitrogen_dioxide"),
        "SO2" to listOf("so2", "sulfur_dioxide"),
        "O3" to listOf("o3", "ozone"),
        "NH3" to listOf("nh3", "ammonia"),
        "CH4" to listOf("ch4", "methane"),
        "Temperature" to listOf("temperature", "temp", "t_c", "temp_c"),
        "Humidity" to listOf("humidity", "rh", "relative_humidity"),
        "Wind Speed" to listOf("wind_speed", "windspeed", "ws", "wind_vel")
    )

    private val TIMESTAMP_PATTERNS = listOf("date", "timestamp", "datetime", "time", "recorded_at")

    fun parseCsv(csvText: String): ParsedDataset {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ParsedDataset(emptyList(), emptyList(), emptyMap(), null, "PM2.5")
        }

        val headerTokens = parseCsvLine(lines[0])
        val rows = lines.drop(1).map { parseCsvLine(it) }.filter { it.size == headerTokens.size }

        val detectedAliases = mutableMapOf<String, String>()
        for (header in headerTokens) {
            val lower = header.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9.]"), "")
            for ((standard, aliases) in ALIAS_PATTERNS) {
                if (aliases.any { lower.contains(it) }) {
                    detectedAliases[header] = standard
                    break
                }
            }
        }

        var timestampCol: String? = null
        for (header in headerTokens) {
            val lower = header.lowercase(Locale.ROOT)
            if (TIMESTAMP_PATTERNS.any { lower.contains(it) }) {
                timestampCol = header
                break
            }
        }

        val targetCandidate = headerTokens.firstOrNull { h ->
            detectedAliases[h] == "PM2.5" || detectedAliases[h] == "PM10"
        } ?: headerTokens.getOrElse(1) { "PM2.5" }

        return ParsedDataset(
            headers = headerTokens,
            rows = rows,
            detectedAliases = detectedAliases,
            detectedTimestampColumn = timestampCol,
            detectedTargetCandidate = targetCandidate
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false
        for (c in line) {
            when {
                c == '"' -> insideQuotes = !insideQuotes
                c == ',' && !insideQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun preprocessAndSplit(
        parsed: ParsedDataset,
        targetColumn: String,
        selectedFeatures: List<String>,
        imputation: ImputationStrategy = ImputationStrategy.FORWARD_FILL,
        trainRatio: Double = 0.8
    ): Pair<DatasetSplit, PreprocessingAudit> {
        val targetIdx = parsed.headers.indexOf(targetColumn)
        require(targetIdx != -1) { "Target column '$targetColumn' not found in dataset headers." }

        val featureIndices = selectedFeatures.map { feat ->
            val idx = parsed.headers.indexOf(feat)
            require(idx != -1) { "Feature column '$feat' not found in dataset headers." }
            idx
        }

        val timeIdx = parsed.detectedTimestampColumn?.let { parsed.headers.indexOf(it) } ?: -1

        // Sort chronologically if timestamp exists
        val sortedRows = if (timeIdx != -1) {
            parsed.rows.sortedBy { row ->
                parseDateSortKey(row.getOrElse(timeIdx) { "" })
            }
        } else {
            parsed.rows
        }

        // Numeric extraction & imputation
        var missingCount = 0
        val numericData = mutableListOf<DoubleArray>()
        val targetValues = mutableListOf<Double>()
        val timestamps = mutableListOf<String>()

        val lastSeenValues = DoubleArray(featureIndices.size) { 0.0 }
        var lastSeenTarget = 0.0
        var hasLastSeen = false

        for (row in sortedRows) {
            val rawTarget = row.getOrNull(targetIdx)?.toDoubleOrNull()
            val targetVal = if (rawTarget != null) {
                lastSeenTarget = rawTarget
                rawTarget
            } else {
                missingCount++
                if (hasLastSeen) lastSeenTarget else 0.0
            }

            val featureRow = DoubleArray(featureIndices.size)
            for (f in featureIndices.indices) {
                val colIdx = featureIndices[f]
                val rawVal = row.getOrNull(colIdx)?.toDoubleOrNull()
                if (rawVal != null) {
                    featureRow[f] = rawVal
                    lastSeenValues[f] = rawVal
                } else {
                    missingCount++
                    featureRow[f] = if (hasLastSeen) lastSeenValues[f] else 0.0
                }
            }

            hasLastSeen = true
            numericData.add(featureRow)
            targetValues.add(targetVal)
            timestamps.add(if (timeIdx != -1) row.getOrElse(timeIdx) { "" } else "T+${numericData.size}h")
        }

        // Chronological Train/Test Split (prevents future leakage)
        val n = numericData.size
        val trainSize = (n * trainRatio).toInt().coerceIn(1, n - 1)
        val testSize = n - trainSize

        val trainXRaw = Array(trainSize) { numericData[it] }
        val trainY = DoubleArray(trainSize) { targetValues[it] }
        val testXRaw = Array(testSize) { numericData[trainSize + it] }
        val testY = DoubleArray(testSize) { targetValues[trainSize + it] }

        val trainTime = timestamps.subList(0, trainSize)
        val testTime = timestamps.subList(trainSize, n)

        // Fit StandardScaler STRICTLY on trainX
        val p = featureIndices.size
        val scalerMeans = DoubleArray(p)
        val scalerStds = DoubleArray(p)

        for (j in 0 until p) {
            var sum = 0.0
            for (i in 0 until trainSize) sum += trainXRaw[i][j]
            val mean = sum / trainSize
            scalerMeans[j] = mean

            var varSum = 0.0
            for (i in 0 until trainSize) {
                val d = trainXRaw[i][j] - mean
                varSum += d * d
            }
            val std = sqrt(varSum / trainSize)
            scalerStds[j] = if (std > 1e-6) std else 1.0
        }

        // Scale trainX
        val trainX = Array(trainSize) { i ->
            DoubleArray(p) { j ->
                (trainXRaw[i][j] - scalerMeans[j]) / scalerStds[j]
            }
        }

        // Scale testX using train scaler parameters (NO leakage)
        val testX = Array(testSize) { i ->
            DoubleArray(p) { j ->
                (testXRaw[i][j] - scalerMeans[j]) / scalerStds[j]
            }
        }

        val split = DatasetSplit(
            trainX = trainX,
            trainY = trainY,
            testX = testX,
            testY = testY,
            featureNames = selectedFeatures,
            targetName = targetColumn,
            trainTimestamps = trainTime,
            testTimestamps = testTime,
            scalerMeans = scalerMeans,
            scalerStds = scalerStds
        )

        val audit = PreprocessingAudit(
            originalRowCount = parsed.rows.size,
            cleanedRowCount = n,
            missingValuesImputed = missingCount,
            duplicatesRemoved = 0,
            selectedFeatures = selectedFeatures,
            targetVariable = targetColumn,
            trainCount = trainSize,
            testCount = testSize,
            chronologicalSortingApplied = (timeIdx != -1)
        )

        return Pair(split, audit)
    }

    private fun parseDateSortKey(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd-MM-yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ROOT)
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (_: Exception) { }
        }
        return dateStr.hashCode().toLong()
    }
}
