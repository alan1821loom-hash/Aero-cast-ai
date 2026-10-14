package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiPoor
import com.example.ui.theme.AqiSatisfactory
import com.example.ui.theme.AqiSevere
import com.example.ui.theme.AqiVeryPoor
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun EnvironmentalLineChart(
    actualData: List<Double>,
    predictedData: List<Double> = emptyList(),
    timestamps: List<String> = emptyList(),
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = CyanSecondary,
    secondaryColor: Color = EmeraldPrimary
) {
    if (actualData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No series data available to render chart.", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = if (selectedIndex != null && selectedIndex!! < actualData.size) {
                        val act = actualData[selectedIndex!!]
                        val pred = predictedData.getOrNull(selectedIndex!!)
                        val time = timestamps.getOrNull(selectedIndex!!) ?: "Point #${selectedIndex!! + 1}"
                        "At $time: Actual = ${String.format("%.1f", act)} $unit" +
                                if (pred != null) " | Pred = ${String.format("%.1f", pred)} $unit" else ""
                    } else {
                        "Showing ${actualData.size} chronological observations"
                    },
                    color = if (selectedIndex != null) EmeraldPrimary else TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Actual", color = TextSecondary, fontSize = 10.sp)
                if (predictedData.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).background(secondaryColor, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Predicted", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(actualData) {
                    detectTapGestures { offset ->
                        val n = actualData.size
                        if (n > 1) {
                            val step = size.width / (n - 1)
                            val idx = ((offset.x + step / 2) / step).toInt().coerceIn(0, n - 1)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paddingBottom = 16f
                val chartHeight = h - paddingBottom

                val allVals = actualData + predictedData
                val minVal = (allVals.minOrNull() ?: 0.0).toFloat() * 0.9f
                val maxVal = max(minVal + 1f, (allVals.maxOrNull() ?: 100.0).toFloat() * 1.1f)
                val range = maxVal - minVal

                // Draw 4 horizontal gridlines
                for (i in 0..3) {
                    val y = chartHeight - (i.toFloat() / 3f) * chartHeight
                    drawLine(
                        color = Color(0xFF1E3356),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val n = actualData.size
                if (n > 1) {
                    val step = w / (n - 1)

                    // Draw actual curve
                    val actualPath = Path()
                    val actualFillPath = Path()

                    for (i in 0 until n) {
                        val x = i * step
                        val normY = (actualData[i].toFloat() - minVal) / range
                        val y = chartHeight - (normY * chartHeight)
                        if (i == 0) {
                            actualPath.moveTo(x, y)
                            actualFillPath.moveTo(x, chartHeight)
                            actualFillPath.lineTo(x, y)
                        } else {
                            actualPath.lineTo(x, y)
                            actualFillPath.lineTo(x, y)
                        }
                    }
                    actualFillPath.lineTo((n - 1) * step, chartHeight)
                    actualFillPath.close()

                    // Gradient fill for actual
                    drawPath(
                        path = actualFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    drawPath(
                        path = actualPath,
                        color = primaryColor,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Draw predicted line if present
                    if (predictedData.isNotEmpty() && predictedData.size == n) {
                        val predPath = Path()
                        for (i in 0 until n) {
                            val x = i * step
                            val normY = (predictedData[i].toFloat() - minVal) / range
                            val y = chartHeight - (normY * chartHeight)
                            if (i == 0) predPath.moveTo(x, y) else predPath.lineTo(x, y)
                        }
                        drawPath(
                            path = predPath,
                            color = secondaryColor,
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }

                    // Highlight selected index
                    if (selectedIndex != null && selectedIndex!! < n) {
                        val x = selectedIndex!! * step
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(x, 0f),
                            end = Offset(x, chartHeight),
                            strokeWidth = 1.5f
                        )
                        val actNorm = (actualData[selectedIndex!!].toFloat() - minVal) / range
                        val actY = chartHeight - (actNorm * chartHeight)
                        drawCircle(color = primaryColor, radius = 5f, center = Offset(x, actY))
                        drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, actY))

                        if (predictedData.isNotEmpty() && selectedIndex!! < predictedData.size) {
                            val predNorm = (predictedData[selectedIndex!!].toFloat() - minVal) / range
                            val predY = chartHeight - (predNorm * chartHeight)
                            drawCircle(color = secondaryColor, radius = 5f, center = Offset(x, predY))
                            drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, predY))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonBarChart(
    items: List<Pair<String, Double>>,
    metricName: String,
    modifier: Modifier = Modifier,
    barColor: Color = CyanSecondary,
    higherIsBetter: Boolean = false
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(metricName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                if (higherIsBetter) "Higher is better" else "Lower is better",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val maxVal = max(0.01, items.maxOf { it.second })

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { (label, value) ->
                val ratio = (value / maxVal).coerceIn(0.02, 1.0)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            String.format("%.3f", value),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(DarkSurfaceElevated, RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.toFloat())
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(barColor.copy(alpha = 0.7f), barColor)
                                    ),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AQIGaugeCard(
    aqi: Int,
    category: String,
    categoryColor: Color,
    governingPollutant: String,
    standardTitle: String,
    advisory: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Air Quality Index", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Surface(
                color = categoryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = category,
                    color = categoryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Radial Semi-circle Gauge
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val radius = w / 2f - 20f
                val center = Offset(w / 2f, h - 10f)

                val strokeWidth = 18f
                // Arc goes from 180 deg to 360 deg (sweep 180 deg)
                // Color bands: Good (0-50, 18 deg), Satisfactory (51-100, 18 deg), Moderate (101-200, 36 deg),
                // Poor (201-300, 36 deg), Very Poor (301-400, 36 deg), Severe (401-500, 36 deg)
                val bands = listOf(
                    Pair(18f, AqiGood),
                    Pair(18f, AqiSatisfactory),
                    Pair(36f, AqiModerate),
                    Pair(36f, AqiPoor),
                    Pair(36f, AqiVeryPoor),
                    Pair(36f, AqiSevere)
                )

                var currentStart = 180f
                for ((sweep, color) in bands) {
                    drawArc(
                        color = color.copy(alpha = 0.35f),
                        startAngle = currentStart,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentStart += sweep
                }

                // Draw active indicator arc up to current AQI
                val aqiClamped = aqi.coerceIn(0, 500)
                val activeSweep = (aqiClamped / 500f) * 180f
                if (activeSweep > 0) {
                    drawArc(
                        color = categoryColor,
                        startAngle = 180f,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth + 4f, cap = StrokeCap.Round)
                    )
                }

                // Needle indicator dot
                val needleAngleRad = Math.toRadians((180f + activeSweep).toDouble())
                val needleX = center.x + (radius * cos(needleAngleRad)).toFloat()
                val needleY = center.y + (radius * sin(needleAngleRad)).toFloat()
                drawCircle(color = Color.White, radius = 7f, center = Offset(needleX, needleY))
                drawCircle(color = categoryColor, radius = 4f, center = Offset(needleX, needleY))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = aqi.toString(),
                    color = categoryColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 38.sp
                )
                Text(
                    text = "AQI (0 - 500)",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Governing: $governingPollutant", color = TextSecondary, fontSize = 11.sp)
            Text(standardTitle, color = TextSecondary, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = advisory,
            color = TextTertiary,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun EnvironmentalKpiCard(
    title: String,
    value: String,
    unit: String,
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(unit, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
