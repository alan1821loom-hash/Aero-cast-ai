package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AirQualityViewModel
import com.example.ui.components.AQIGaugeCard
import com.example.ui.components.ComparisonBarChart
import com.example.ui.components.EnvironmentalKpiCard
import com.example.ui.components.EnvironmentalLineChart
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeTertiary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DashboardScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val dataset by viewModel.latestDataset.collectAsState()
    val parsed by viewModel.parsedDataset.collectAsState()
    val models by viewModel.allModels.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()
    val aqiResult by viewModel.latestAqi.collectAsState()
    val activeMetrics by viewModel.activeEvaluationMetrics.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val trainingStatus by viewModel.trainingStatus.collectAsState()
    val isSynthetic by viewModel.isSyntheticBenchmark.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Platform Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Environmental Intelligence",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Air Quality Prediction • B.Sc. AI & Data Science",
                        color = CyanSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = if (activeAlerts.isNotEmpty()) RedDanger.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeAlerts.isNotEmpty()) RedDanger.copy(alpha = 0.6f) else EmeraldPrimary.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (activeAlerts.isNotEmpty()) RedDanger else EmeraldPrimary,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeAlerts.isNotEmpty()) "${activeAlerts.size} Alerts Active" else "Nominal State",
                            color = if (activeAlerts.isNotEmpty()) RedDanger else EmeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Dataset Provenance Card: Explicitly distinguishes real vs synthetic demonstration data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSynthetic) DarkSurfaceVariant else DarkSurfaceElevated
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSynthetic) AmberWarning.copy(alpha = 0.5f) else EmeraldPrimary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSynthetic) Icons.Default.Science else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSynthetic) AmberWarning else EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSynthetic) "DATASET PROVENANCE: SYNTHETIC BENCHMARK" else "DATASET PROVENANCE: REAL OBSERVATIONS",
                            color = if (isSynthetic) AmberWarning else EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (isSynthetic) {
                                "Pre-calibrated 120-hour academic benchmark dataset with realistic diurnal temperature and rush-hour emission dynamics."
                            } else {
                                "User-uploaded atmospheric measurements loaded for real empirical model fitting and evaluation."
                            },
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // Live Processing Banner if active
        if (isProcessing && trainingStatus.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, CyanSecondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyanSecondary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = trainingStatus,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // KPI Metric Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rowCount = dataset?.rowCount ?: 0
                    EnvironmentalKpiCard(
                        title = "Dataset Records",
                        value = if (rowCount > 0) "$rowCount" else "Empty",
                        unit = "rows",
                        statusText = if (rowCount > 0) "Synchronized" else "No dataset",
                        statusColor = if (rowCount > 0) EmeraldPrimary else AmberWarning,
                        modifier = Modifier.weight(1f)
                    )

                    val trainedCount = models.size
                    EnvironmentalKpiCard(
                        title = "Trained ML Models",
                        value = "$trainedCount",
                        unit = "models",
                        statusText = if (trainedCount > 0) "Evaluated" else "Needs training",
                        statusColor = if (trainedCount > 0) CyanSecondary else AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val latestPm25 = aqiResult?.subIndices?.get("PM2.5")?.toDouble()
                        ?: parsed?.rows?.lastOrNull()?.getOrNull(4)?.toDoubleOrNull()
                    EnvironmentalKpiCard(
                        title = "Latest PM2.5",
                        value = if (latestPm25 != null) String.format("%.1f", latestPm25) else "--",
                        unit = "µg/m³",
                        statusText = if (latestPm25 != null && latestPm25 > 60.0) "Elevated" else "Acceptable",
                        statusColor = if (latestPm25 != null && latestPm25 > 60.0) RedDanger else EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    val latestPm10 = parsed?.rows?.lastOrNull()?.getOrNull(5)?.toDoubleOrNull()
                    EnvironmentalKpiCard(
                        title = "Latest PM10",
                        value = if (latestPm10 != null) String.format("%.1f", latestPm10) else "--",
                        unit = "µg/m³",
                        statusText = if (latestPm10 != null && latestPm10 > 100.0) "Elevated" else "Normal",
                        statusColor = if (latestPm10 != null && latestPm10 > 100.0) AmberWarning else EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // AQI Radial Gauge
        item {
            if (aqiResult != null) {
                AQIGaugeCard(
                    aqi = aqiResult!!.aqiValue,
                    category = aqiResult!!.category,
                    categoryColor = aqiResult!!.color,
                    governingPollutant = aqiResult!!.governingPollutant,
                    standardTitle = aqiResult!!.standardUsed.label,
                    advisory = aqiResult!!.advisory
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AQI calculations require dataset with pollutant columns.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Functional Primary Actions: Train ML button actually trains model & Predict button generates forecast
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Operational Controls",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Train ML Button: Actually executes training on the available dataset
                        Button(
                            onClick = {
                                viewModel.trainPrimaryModelQuickly()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isProcessing) "Training..." else "Train ML",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Predict Button: Generates prediction or navigates to prediction studio
                        Button(
                            onClick = {
                                onNavigate(NavScreen.PREDICTION)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Predict", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (models.isNotEmpty()) "${models.size} models trained" else "Ready to train algorithms",
                            color = if (models.isNotEmpty()) EmeraldPrimary else TextSecondary,
                            fontSize = 11.sp
                        )
                        OutlinedButton(
                            onClick = { onNavigate(NavScreen.MODELS) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Open Training Studio", fontSize = 11.sp, color = CyanSecondary)
                        }
                    }
                }
            }
        }

        // Live Empirical Metrics Snapshot (if models are trained)
        if (models.isNotEmpty()) {
            item {
                val bestModel = models.maxByOrNull { it.r2 }
                if (bestModel != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LimeTertiary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LimeTertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Held-Out Evaluation Metrics: ${bestModel.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Target: ${bestModel.targetPollutant}",
                                        color = EmeraldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("R² Score", color = TextSecondary, fontSize = 10.sp)
                                    Text(String.format("%.4f", bestModel.r2), color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("RMSE (µg/m³)", color = TextSecondary, fontSize = 10.sp)
                                    Text(String.format("%.2f", bestModel.rmse), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("MAE (µg/m³)", color = TextSecondary, fontSize = 10.sp)
                                    Text(String.format("%.2f", bestModel.mae), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("Test Split", color = TextSecondary, fontSize = 10.sp)
                                    Text("${bestModel.testSampleSize} rows", color = CyanSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Historical Trend Charts
        item {
            val pm25Values = parsed?.rows?.mapNotNull { row ->
                val idx = parsed?.headers?.indexOfFirst { it.contains("PM2.5", ignoreCase = true) || it.contains("PM25", ignoreCase = true) } ?: 4
                row.getOrNull(idx)?.toDoubleOrNull()
            } ?: emptyList()

            val timestamps = parsed?.rows?.mapNotNull { it.getOrNull(0) } ?: emptyList()

            EnvironmentalLineChart(
                actualData = pm25Values.takeLast(48),
                timestamps = timestamps.takeLast(48),
                title = "PM2.5 Historical Trend (Last 48 Hours)",
                unit = "µg/m³",
                primaryColor = CyanSecondary
            )
        }

        // Actual vs Predicted Chart if models have been evaluated
        item {
            val bestModelKey = activeMetrics.keys.firstOrNull()
            val metrics = if (bestModelKey != null) activeMetrics[bestModelKey] else null
            if (metrics != null) {
                EnvironmentalLineChart(
                    actualData = metrics.actualValues.take(30),
                    predictedData = metrics.predictedValues.take(30),
                    timestamps = metrics.testTimestamps.take(30),
                    title = "Model Evaluation: Actual vs Predicted ($bestModelKey)",
                    unit = "µg/m³",
                    primaryColor = CyanSecondary,
                    secondaryColor = EmeraldPrimary
                )
            }
        }

        // Pollutant Concentration Snapshot Bar Chart
        item {
            val lastRow = parsed?.rows?.lastOrNull()
            if (lastRow != null && parsed != null) {
                val pollutants = listOf("PM2.5", "PM10", "NO2", "SO2", "CO", "O3")
                val pairs = pollutants.mapNotNull { p ->
                    val idx = parsed!!.headers.indexOfFirst { it.contains(p, ignoreCase = true) }
                    if (idx != -1) {
                        val v = lastRow.getOrNull(idx)?.toDoubleOrNull() ?: 0.0
                        Pair(p, v)
                    } else null
                }
                if (pairs.isNotEmpty()) {
                    ComparisonBarChart(
                        items = pairs,
                        metricName = "Current Pollutant Concentrations (µg/m³)",
                        barColor = EmeraldPrimary
                    )
                }
            }
        }

        // Active Alerts Warning Card
        if (activeAlerts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedDanger.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, RedDanger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Environmental Alerts (${activeAlerts.size})",
                            color = RedDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeAlerts.first().message,
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onNavigate(NavScreen.ALERTS) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Review Alerts Feed", color = RedDanger, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
