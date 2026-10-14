package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.AQICalculator
import com.example.ui.AirQualityViewModel
import com.example.ui.components.AQIGaugeCard
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiPoor
import com.example.ui.theme.AqiSatisfactory
import com.example.ui.theme.AqiSevere
import com.example.ui.theme.AqiVeryPoor
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun RiskAQIScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val aqiResult by viewModel.latestAqi.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("AQI Standards & Risk Classification", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Standardized CPCB (India) and US EPA breakpoint calculation framework",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Active AQI Card
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
            }
        }

        // Mathematical formulation card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Piecewise Linear Interpolation Formula", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "I_p = ((I_hi - I_lo) / (BP_hi - BP_lo)) * (C_p - BP_lo) + I_lo",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• C_p: Measured or predicted truncated concentration of pollutant p\n" +
                                "• BP_hi, BP_lo: Breakpoint concentrations enclosing C_p\n" +
                                "• I_hi, I_lo: Corresponding Air Quality Index limits\n" +
                                "• Overall AQI = max(I_p1, I_p2, ... I_pn) [Governing pollutant principle]",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Breakpoint Standard Table
        item {
            Text("CPCB National AQI Category Breakpoints (PM2.5 & PM10):", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        val brackets = listOf(
            Triple("Good (0 - 50)", AqiGood, "PM2.5: 0–30 | PM10: 0–50 µg/m³\nMinimal health impact."),
            Triple("Satisfactory (51 - 100)", AqiSatisfactory, "PM2.5: 31–60 | PM10: 51–100 µg/m³\nMinor breathing discomfort to sensitive people."),
            Triple("Moderate (101 - 200)", AqiModerate, "PM2.5: 61–90 | PM10: 101–250 µg/m³\nDiscomfort to people with asthma and heart conditions."),
            Triple("Poor (201 - 300)", AqiPoor, "PM2.5: 91–120 | PM10: 251–350 µg/m³\nBreathing discomfort to most people on prolonged exposure."),
            Triple("Very Poor (301 - 400)", AqiVeryPoor, "PM2.5: 121–250 | PM10: 351–430 µg/m³\nRespiratory illness on prolonged exposure."),
            Triple("Severe (401 - 500)", AqiSevere, "PM2.5: >250 | PM10: >430 µg/m³\nSevere emergency impact on healthy and sensitive persons.")
        )

        items(brackets.size) { idx ->
            val (name, color, details) = brackets[idx]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(name, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(details, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}
