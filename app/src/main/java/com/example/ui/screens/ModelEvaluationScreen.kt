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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AirQualityViewModel
import com.example.ui.components.ComparisonBarChart
import com.example.ui.components.EnvironmentalLineChart
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import androidx.compose.material3.HorizontalDivider
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeTertiary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ModelEvaluationScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val models by viewModel.allModels.collectAsState()
    val activeMetrics by viewModel.activeEvaluationMetrics.collectAsState()
    val importancesMap by viewModel.featureImportances.collectAsState()

    var selectedModelKey by remember(activeMetrics) {
        mutableStateOf(activeMetrics.keys.firstOrNull() ?: "")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Model Evaluation & Comparison", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Rigorous performance evaluation calculated from genuine chronological test sets",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (models.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No models trained yet.", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Train algorithms from the ML Training Studio to compare empirical metrics.", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onNavigate(NavScreen.MODELS) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go to Model Training", fontSize = 11.sp)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // Best Model Highlight Card
        item {
            val bestModel = models.maxByOrNull { it.r2 }
            if (bestModel != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LimeTertiary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LimeTertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = LimeTertiary, modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Best Performing Model", color = LimeTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Target: ${bestModel.targetPollutant}",
                                        color = EmeraldPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(bestModel.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "R²: ${String.format("%.4f", bestModel.r2)} | RMSE: ${String.format("%.2f", bestModel.rmse)} µg/m³ | Train: ${bestModel.trainTimeMs}ms",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Model Comparison Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Model Comparison Leaderboard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.horizontalScroll(scroll)) {
                        // Headers
                        Row(
                            modifier = Modifier
                                .background(DarkSurfaceElevated, RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            Text("Algorithm", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(150.dp).padding(horizontal = 6.dp))
                            Text("R² Score", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                            Text("RMSE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                            Text("MAE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                            Text("Train Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        }

                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                        models.sortedByDescending { it.r2 }.forEachIndexed { idx, m ->
                            Row(
                                modifier = Modifier
                                    .background(if (idx % 2 == 0) DarkSurfaceVariant else DarkSurface)
                                    .clickable { selectedModelKey = m.algorithm }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    m.name,
                                    color = if (m.algorithm == selectedModelKey) CyanSecondary else TextPrimary,
                                    fontWeight = if (m.algorithm == selectedModelKey) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(150.dp).padding(horizontal = 6.dp)
                                )
                                Text(
                                    String.format("%.3f", m.r2),
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(75.dp)
                                )
                                Text(
                                    String.format("%.2f", m.rmse),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(75.dp)
                                )
                                Text(
                                    String.format("%.2f", m.mae),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(75.dp)
                                )
                                Text(
                                    "${m.trainTimeMs}ms",
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Performance Bar Comparison
        item {
            val r2Pairs = models.map { Pair(it.name.replace(" Regressor", ""), it.r2) }
            ComparisonBarChart(
                items = r2Pairs,
                metricName = "R² Coefficient of Determination (Higher is Better)",
                barColor = EmeraldPrimary,
                higherIsBetter = true
            )
        }

        item {
            val rmsePairs = models.map { Pair(it.name.replace(" Regressor", ""), it.rmse) }
            ComparisonBarChart(
                items = rmsePairs,
                metricName = "Root Mean Squared Error (RMSE) [µg/m³]",
                barColor = CyanSecondary,
                higherIsBetter = false
            )
        }

        // Detailed Selected Model Actual vs Predicted Chart
        item {
            val metrics = activeMetrics[selectedModelKey]
                ?: activeMetrics.values.firstOrNull()

            if (metrics != null) {
                EnvironmentalLineChart(
                    actualData = metrics.actualValues.take(35),
                    predictedData = metrics.predictedValues.take(35),
                    timestamps = metrics.testTimestamps.take(35),
                    title = "Actual vs Predicted Trajectory (${selectedModelKey.ifBlank { "Model" }})",
                    unit = "µg/m³",
                    primaryColor = CyanSecondary,
                    secondaryColor = EmeraldPrimary
                )
            }
        }

        // Residual Analysis Chart
        item {
            val metrics = activeMetrics[selectedModelKey]
                ?: activeMetrics.values.firstOrNull()

            if (metrics != null) {
                EnvironmentalLineChart(
                    actualData = metrics.residualValues.take(35),
                    timestamps = metrics.testTimestamps.take(35),
                    title = "Residual Errors: (Actual - Predicted)",
                    unit = "Error",
                    primaryColor = AmberWarning
                )
            }
        }

        // Feature Importance Chart (if available for selected model)
        item {
            val importances = importancesMap[selectedModelKey]
                ?: importancesMap.values.firstOrNull()

            if (importances != null && importances.isNotEmpty()) {
                val pairs = importances.map { Pair(it.featureName, it.score) }
                ComparisonBarChart(
                    items = pairs,
                    metricName = "Feature Importance Weight",
                    barColor = LimeTertiary,
                    higherIsBetter = true
                )
            }
        }

        // Proceed to Prediction Button
        item {
            Button(
                onClick = { onNavigate(NavScreen.PREDICTION) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Make Future Air Quality Predictions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
