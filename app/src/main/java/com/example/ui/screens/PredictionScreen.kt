package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.AQICalculator
import com.example.ml.AQIResult
import com.example.ml.ModelAlgorithm
import com.example.ui.AirQualityViewModel
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun PredictionScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val models by viewModel.allModels.collectAsState()
    val audit by viewModel.auditSummary.collectAsState()
    val parsed by viewModel.parsedDataset.collectAsState()
    val history by viewModel.predictionHistory.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedAlgoKey by remember(models) {
        mutableStateOf(models.firstOrNull()?.algorithm ?: ModelAlgorithm.RANDOM_FOREST.name)
    }

    var selectedTargetPollutant by remember { mutableStateOf("PM2.5") }
    var selectedTabMode by remember { mutableIntStateOf(0) } // 0 = Future Forecast, 1 = What-If Scenario

    var horizonHours by remember { mutableIntStateOf(6) }

    // What-if feature sliders
    var tempC by remember { mutableStateOf(26.0f) }
    var humidityPct by remember { mutableStateOf(60.0f) }
    var windKmh by remember { mutableStateOf(9.0f) }

    var latestPredValue by remember { mutableStateOf<Double?>(null) }
    var latestPredAqi by remember { mutableStateOf<AQIResult?>(null) }
    var lastPredictionModeLabel by remember { mutableStateOf("") }
    var lastInputBreakdown by remember { mutableStateOf("") }

    val isCurrentModelTrained = viewModel.isModelTrained(selectedAlgoKey)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Air Quality Prediction & Forecast", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Predict future PM2.5, PM10, and AQI concentrations with multi-horizon time-series forecasting",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Target Pollutant Selector (PM2.5 vs PM10 vs AQI)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("1. Target Prediction Parameter", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PM2.5", "PM10").forEach { target ->
                            val isSelected = target == selectedTargetPollutant
                            Surface(
                                color = if (isSelected) EmeraldPrimary else DarkSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTargetPollutant = target }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$target (µg/m³)",
                                        color = if (isSelected) DarkBg else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Model Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2. Forecasting Algorithm", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = if (isCurrentModelTrained) "Status: Ready" else "Status: Untrained",
                            color = if (isCurrentModelTrained) EmeraldPrimary else AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelAlgorithm.values().forEach { algo ->
                            val isSelected = algo.name == selectedAlgoKey
                            val trained = viewModel.isModelTrained(algo.name)

                            Surface(
                                color = if (isSelected) CyanSecondary.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanSecondary else DarkBorder
                                ),
                                modifier = Modifier.clickable { selectedAlgoKey = algo.name }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        algo.displayName,
                                        color = if (isSelected) CyanSecondary else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        if (trained) "Trained ✓" else "Not Trained",
                                        color = if (trained) EmeraldPrimary else AmberWarning,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Informative card if selected model is not yet trained
        if (!isCurrentModelTrained) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Model '$selectedAlgoKey' is not trained yet",
                                color = AmberWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Inferences cannot be fabricated. You must train this algorithm on the available dataset first.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val algoEnum = ModelAlgorithm.values().firstOrNull { it.name == selectedAlgoKey }
                                    ?: ModelAlgorithm.RANDOM_FOREST
                                viewModel.trainModel(algoEnum)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Train '$selectedAlgoKey' Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Mode Tabs: Chronological Forecast vs What-If Scenario
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("3. Prediction Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = selectedTabMode,
                        containerColor = DarkSurface,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabMode]),
                                color = EmeraldPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabMode == 0,
                            onClick = { selectedTabMode = 0 },
                            text = { Text("Future Forecast (+h)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTabMode == 1,
                            onClick = { selectedTabMode = 1 },
                            text = { Text("What-If Scenario", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTabMode == 0) {
                        // Chronological Future Forecast
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Forecast Horizon", color = TextSecondary, fontSize = 11.sp)
                            Text("+${horizonHours} Hours Ahead", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1, 6, 12, 24, 48, 72).forEach { h ->
                                val isSelected = h == horizonHours
                                Surface(
                                    color = if (isSelected) EmeraldPrimary else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { horizonHours = h }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${h}h",
                                            color = if (isSelected) DarkBg else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val latestTime = parsed?.rows?.lastOrNull()?.getOrNull(0) ?: "Latest Observation"
                        Text(
                            "Extrapolates from chronological baseline sequence ($latestTime) with diurnal atmospheric adjustments.",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    } else {
                        // What-If Scenario with User-entered measurements
                        Text(
                            "Enter hypothetical meteorological conditions to evaluate pollutant response:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Temp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ambient Temperature", color = TextSecondary, fontSize = 11.sp)
                            Text("${String.format("%.1f", tempC)} °C", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = tempC,
                            onValueChange = { tempC = it },
                            valueRange = 10f..45f,
                            colors = SliderDefaults.colors(thumbColor = CyanSecondary, activeTrackColor = CyanSecondary)
                        )

                        // Humidity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Relative Humidity", color = TextSecondary, fontSize = 11.sp)
                            Text("${String.format("%.0f", humidityPct)} %", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = humidityPct,
                            onValueChange = { humidityPct = it },
                            valueRange = 20f..100f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                        )

                        // Wind
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Wind Speed (Dispersion)", color = TextSecondary, fontSize = 11.sp)
                            Text("${String.format("%.1f", windKmh)} km/h", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Slider(
                            value = windKmh,
                            onValueChange = { windKmh = it },
                            valueRange = 1f..35f,
                            colors = SliderDefaults.colors(thumbColor = CyanSecondary, activeTrackColor = CyanSecondary)
                        )
                    }
                }
            }
        }

        // Generate Prediction Action Button
        item {
            Button(
                onClick = {
                    scope.launch {
                        val featureMap = if (selectedTabMode == 1) {
                            mapOf(
                                "Temperature_C" to tempC.toDouble(),
                                "Humidity_Pct" to humidityPct.toDouble(),
                                "WindSpeed_kmh" to windKmh.toDouble(),
                                "Temperature" to tempC.toDouble(),
                                "Humidity" to humidityPct.toDouble(),
                                "Wind Speed" to windKmh.toDouble()
                            )
                        } else {
                            emptyMap()
                        }
                        try {
                            val (pred, aqi) = viewModel.predictAirQuality(
                                algorithmKey = selectedAlgoKey,
                                targetPollutant = selectedTargetPollutant,
                                horizonHours = horizonHours,
                                customFeatures = featureMap,
                                isScenarioMode = (selectedTabMode == 1)
                            )
                            latestPredValue = pred
                            latestPredAqi = aqi
                            lastPredictionModeLabel = if (selectedTabMode == 0) {
                                "CHRONOLOGICAL FUTURE FORECAST (+${horizonHours}h)"
                            } else {
                                "WHAT-IF METEOROLOGICAL SCENARIO"
                            }
                            lastInputBreakdown = if (selectedTabMode == 0) {
                                "Baseline: Latest dataset observation sequence | Horizon: +${horizonHours} Hours"
                            } else {
                                "User Input: Temperature = ${String.format("%.1f", tempC)}°C, Humidity = ${String.format("%.0f", humidityPct)}%, Wind = ${String.format("%.1f", windKmh)} km/h"
                            }
                        } catch (e: Exception) {
                            // Handled via ViewModel notification
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                enabled = isCurrentModelTrained && !isProcessing
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentModelTrained) "Generate $selectedTargetPollutant Prediction" else "Model Untrained — Train First",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Prediction Results Presentation Card
        if (latestPredValue != null && latestPredAqi != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, latestPredAqi!!.color),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lastPredictionModeLabel,
                                color = CyanSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = latestPredAqi!!.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = latestPredAqi!!.category,
                                    color = latestPredAqi!!.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.1f", latestPredValue!!),
                                color = TextPrimary,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "µg/m³ ($selectedTargetPollutant)",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Calculated Sub-Index: AQI ${latestPredAqi!!.aqiValue}", color = latestPredAqi!!.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Model: $selectedAlgoKey", color = TextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Input Breakdown Box
                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Input Parameters Used:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(lastInputBreakdown, color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = latestPredAqi!!.advisory,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Multi-Model Consensus Comparison
        if (models.size > 1 && latestPredValue != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Multi-Model Consensus Breakdown", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Comparing predictions across all currently trained architectures:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            models.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceElevated, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.name, color = TextPrimary, fontSize = 11.sp)
                                    Text(
                                        "R² = ${String.format("%.3f", m.r2)}",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Predictions History
        if (history.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recent Prediction History", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.take(4).forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${p.targetPollutant}: ${String.format("%.1f", p.predictedValue)} µg/m³ (AQI ${p.aqiValue})",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text("${p.modelName} | ${p.inputDetails}", color = TextSecondary, fontSize = 10.sp)
                                }
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        p.aqiCategory,
                                        color = EmeraldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
