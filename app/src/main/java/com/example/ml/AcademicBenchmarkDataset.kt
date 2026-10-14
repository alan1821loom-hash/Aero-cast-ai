package com.example.ml

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object AcademicBenchmarkDataset {

    const val DATASET_NAME = "Academic_Benchmark_Synthetic_Hourly_AirQuality.csv"
    const val DATASET_METADATA_DESCRIPTION =
        "[SYNTHETIC DEMONSTRATION DATASET] 120 hourly atmospheric records designed for academic model training & evaluation. Contains PM2.5, PM10, NO2, SO2, CO, O3, Temperature, Humidity, Wind Speed."

    fun generateCsv(): String {
        val sb = StringBuilder()
        sb.append("Timestamp,Temperature_C,Humidity_Pct,WindSpeed_kmh,PM2.5_ugm3,PM10_ugm3,NO2_ugm3,SO2_ugm3,CO_mgm3,O3_ugm3\n")

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val rng = Random(42)

        for (hour in 0 until 120) {
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            // Diurnal temperature cycle: lowest at 05:00, peak at 14:00
            val tempCycle = 22.0 + 7.0 * sin((hourOfDay - 8.0) * Math.PI / 12.0)
            val temperature = (tempCycle + rng.nextDouble(-1.2, 1.2)).coerceIn(12.0, 38.0)

            // Humidity inversely related to temp
            val humidityCycle = 68.0 - 20.0 * sin((hourOfDay - 8.0) * Math.PI / 12.0)
            val humidity = (humidityCycle + rng.nextDouble(-4.0, 4.0)).coerceIn(25.0, 95.0)

            // Wind speed: higher in afternoon
            val windBase = 8.5 + 4.0 * sin((hourOfDay - 10.0) * Math.PI / 12.0)
            val windSpeed = (windBase + rng.nextDouble(-2.0, 3.5)).coerceIn(1.5, 24.0)

            // Traffic rush hours at 08:00 - 10:00 and 18:00 - 21:00
            val isMorningPeak = hourOfDay in 7..10
            val isEveningPeak = hourOfDay in 17..21
            val trafficFactor = if (isMorningPeak) 1.7 else if (isEveningPeak) 1.9 else 0.85

            // Wind dispersion factor: high wind disperses PM
            val dispersionFactor = 1.0 / (1.0 + (windSpeed / 12.0))

            // PM2.5 (ug/m3)
            val pm25Base = 42.0 * trafficFactor * dispersionFactor + (humidity / 15.0)
            val pm25 = (pm25Base + rng.nextDouble(-5.0, 6.0)).coerceIn(8.0, 185.0)

            // PM10 (ug/m3) - roughly 1.6x - 2.1x of PM2.5
            val pm10Base = pm25 * 1.85 + (windSpeed * 1.5)
            val pm10 = (pm10Base + rng.nextDouble(-8.0, 10.0)).coerceIn(18.0, 340.0)

            // NO2 (ug/m3) - strongly correlated with vehicle emissions
            val no2Base = 32.0 * trafficFactor * dispersionFactor
            val no2 = (no2Base + rng.nextDouble(-4.0, 5.0)).coerceIn(6.0, 120.0)

            // SO2 (ug/m3) - industrial background
            val so2Base = 14.0 + 3.0 * sin(hour * 0.1)
            val so2 = (so2Base + rng.nextDouble(-2.5, 3.0)).coerceIn(3.0, 45.0)

            // CO (mg/m3)
            val coBase = 0.8 * trafficFactor * dispersionFactor
            val co = (coBase + rng.nextDouble(-0.15, 0.2)).coerceIn(0.2, 3.8)

            // O3 (ug/m3) - photochemical formation in afternoon sun
            val o3Base = if (hourOfDay in 11..17) 55.0 + 20.0 * sin((hourOfDay - 11.0) * Math.PI / 6.0) else 15.0
            val o3 = (o3Base + rng.nextDouble(-4.0, 6.0)).coerceIn(5.0, 110.0)

            val timeStr = sdf.format(cal.time)
            sb.append(
                String.format(
                    Locale.US,
                    "%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.2f,%.1f\n",
                    timeStr, temperature, humidity, windSpeed, pm25, pm10, no2, so2, co, o3
                )
            )

            cal.add(Calendar.HOUR_OF_DAY, 1)
        }

        return sb.toString()
    }
}
