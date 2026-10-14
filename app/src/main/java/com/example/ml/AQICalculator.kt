package com.example.ml

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiPoor
import com.example.ui.theme.AqiSatisfactory
import com.example.ui.theme.AqiSevere
import com.example.ui.theme.AqiVeryPoor
import kotlin.math.roundToInt

enum class AQIStandard(val label: String, val authority: String) {
    CPCB_INDIA("National AQI (NAQI)", "Central Pollution Control Board (India)"),
    US_EPA("Air Quality Index (AQI)", "United States Environmental Protection Agency")
}

data class AQIResult(
    val aqiValue: Int,
    val category: String,
    val color: Color,
    val governingPollutant: String,
    val standardUsed: AQIStandard,
    val advisory: String,
    val subIndices: Map<String, Int>
)

object AQICalculator {

    // CPCB Breakpoints: (BP_lo, BP_hi, I_lo, I_hi)
    private val CPCB_PM25_BREAKPOINTS = listOf(
        BreakPoint(0.0, 30.0, 0, 50),
        BreakPoint(31.0, 60.0, 51, 100),
        BreakPoint(61.0, 90.0, 101, 200),
        BreakPoint(91.0, 120.0, 201, 300),
        BreakPoint(121.0, 250.0, 301, 400),
        BreakPoint(250.1, 500.0, 401, 500)
    )

    private val CPCB_PM10_BREAKPOINTS = listOf(
        BreakPoint(0.0, 50.0, 0, 50),
        BreakPoint(51.0, 100.0, 51, 100),
        BreakPoint(101.0, 250.0, 101, 200),
        BreakPoint(251.0, 350.0, 201, 300),
        BreakPoint(351.0, 430.0, 301, 400),
        BreakPoint(430.1, 600.0, 401, 500)
    )

    private val CPCB_NO2_BREAKPOINTS = listOf(
        BreakPoint(0.0, 40.0, 0, 50),
        BreakPoint(41.0, 80.0, 51, 100),
        BreakPoint(81.0, 180.0, 101, 200),
        BreakPoint(181.0, 280.0, 201, 300),
        BreakPoint(281.0, 400.0, 301, 400),
        BreakPoint(400.1, 800.0, 401, 500)
    )

    private val CPCB_SO2_BREAKPOINTS = listOf(
        BreakPoint(0.0, 40.0, 0, 50),
        BreakPoint(41.0, 80.0, 51, 100),
        BreakPoint(81.0, 380.0, 101, 200),
        BreakPoint(381.0, 800.0, 201, 300),
        BreakPoint(801.0, 1600.0, 301, 400),
        BreakPoint(1600.1, 2000.0, 401, 500)
    )

    private val CPCB_CO_BREAKPOINTS = listOf(
        BreakPoint(0.0, 1.0, 0, 50),
        BreakPoint(1.1, 2.0, 51, 100),
        BreakPoint(2.1, 10.0, 101, 200),
        BreakPoint(10.1, 17.0, 201, 300),
        BreakPoint(17.1, 34.0, 301, 400),
        BreakPoint(34.1, 50.0, 401, 500)
    )

    private val CPCB_O3_BREAKPOINTS = listOf(
        BreakPoint(0.0, 50.0, 0, 50),
        BreakPoint(51.0, 100.0, 51, 100),
        BreakPoint(101.0, 168.0, 101, 200),
        BreakPoint(169.0, 208.0, 201, 300),
        BreakPoint(209.0, 748.0, 301, 400),
        BreakPoint(748.1, 1000.0, 401, 500)
    )

    private data class BreakPoint(
        val bpLo: Double,
        val bpHi: Double,
        val iLo: Int,
        val iHi: Int
    )

    fun calculateAqi(
        pollutantValues: Map<String, Double>,
        standard: AQIStandard = AQIStandard.CPCB_INDIA
    ): AQIResult {
        val subIndices = mutableMapOf<String, Int>()

        for ((pollutant, conc) in pollutantValues) {
            val bpList = when (pollutant.uppercase()) {
                "PM2.5", "PM25" -> CPCB_PM25_BREAKPOINTS
                "PM10" -> CPCB_PM10_BREAKPOINTS
                "NO2" -> CPCB_NO2_BREAKPOINTS
                "SO2" -> CPCB_SO2_BREAKPOINTS
                "CO" -> CPCB_CO_BREAKPOINTS
                "O3" -> CPCB_O3_BREAKPOINTS
                else -> null
            }
            if (bpList != null && conc >= 0.0) {
                val index = computeSubIndex(conc, bpList)
                subIndices[pollutant] = index
            }
        }

        if (subIndices.isEmpty()) {
            return AQIResult(
                aqiValue = 0,
                category = "AQI Unavailable",
                color = Color.Gray,
                governingPollutant = "None",
                standardUsed = standard,
                advisory = "Insufficient pollutant parameters to compute verified sub-index.",
                subIndices = emptyMap()
            )
        }

        // Governing AQI is the maximum sub-index (CPCB & EPA standard rule)
        val governingEntry = subIndices.maxByOrNull { it.value }!!
        val maxAqi = governingEntry.value
        val governingPollutant = governingEntry.key

        val (category, color, advisory) = categorizeAqi(maxAqi)

        return AQIResult(
            aqiValue = maxAqi,
            category = category,
            color = color,
            governingPollutant = governingPollutant,
            standardUsed = standard,
            advisory = advisory,
            subIndices = subIndices
        )
    }

    private fun computeSubIndex(conc: Double, breakpoints: List<BreakPoint>): Int {
        val bp = breakpoints.firstOrNull { conc in it.bpLo..it.bpHi }
            ?: if (conc < breakpoints.first().bpLo) breakpoints.first() else breakpoints.last()

        val subIndex = ((bp.iHi - bp.iLo).toDouble() / (bp.bpHi - bp.bpLo)) * (conc - bp.bpLo) + bp.iLo
        return subIndex.roundToInt().coerceIn(0, 500)
    }

    fun categorizeAqi(aqi: Int): Triple<String, Color, String> {
        return when {
            aqi <= 50 -> Triple(
                "Good",
                AqiGood,
                "Air quality is considered satisfactory, and air pollution poses little or no risk."
            )
            aqi <= 100 -> Triple(
                "Satisfactory",
                AqiSatisfactory,
                "Air quality is acceptable; however, a small number of sensitive individuals may experience minor breathing discomfort."
            )
            aqi <= 200 -> Triple(
                "Moderate",
                AqiModerate,
                "Breathing discomfort to the people with lung, asthma and heart diseases on prolonged exposure."
            )
            aqi <= 300 -> Triple(
                "Poor",
                AqiPoor,
                "Breathing discomfort to most people on prolonged exposure; avoid strenuous outdoor exercise."
            )
            aqi <= 400 -> Triple(
                "Very Poor",
                AqiVeryPoor,
                "Respiratory illness on prolonged exposure. Significant health impacts for vulnerable populations."
            )
            else -> Triple(
                "Severe",
                AqiSevere,
                "Emergency health warning: Affects healthy people and seriously impacts those with existing diseases."
            )
        }
    }
}
