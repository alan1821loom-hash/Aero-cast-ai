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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.EnvironmentalKpiCard
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

@Composable
fun SensorMonitoringScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val sensorState by viewModel.sensorState.collectAsState()
    val sensorLogs by viewModel.sensorLogs.collectAsState()

    var endpointUrl by remember { mutableStateOf(sensorState.endpointUrl) }
    var apiKey by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("IoT Sensor Integration", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Real-time sensor telemetry connection & academic test bed integration",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Real Sensor Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (sensorState.isConnected) DarkSurfaceElevated else DarkSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (sensorState.isConnected) EmeraldPrimary else DarkBorder
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (sensorState.isConnected) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                contentDescription = null,
                                tint = if (sensorState.isConnected) EmeraldPrimary else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (sensorState.isConnected) "Telemetry Stream Active" else "No Live Hardware Connected",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            color = if (sensorState.isConnected) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (sensorState.isAcademicTestBedActive) "DEMONSTRATION STREAM" else if (sensorState.isConnected) "LIVE HARWARE" else "STANDBY",
                                color = if (sensorState.isConnected) EmeraldPrimary else AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!sensorState.isConnected) {
                        Text(
                            "Physical IoT hardware (e.g. ESP32, PMS5003, MQ135) or a real telemetry gateway is currently unattached. You can toggle the Academic Test Bed stream below for academic defense demonstration.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Sensor Node ID", color = TextSecondary, fontSize = 10.sp)
                                Text(sensorState.sensorId, color = CyanSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Data Freshness", color = TextSecondary, fontSize = 10.sp)
                                Text("1.2s Heartbeat", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Protocol", color = TextSecondary, fontSize = 10.sp)
                                Text("HTTPS / MQTT", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Academic Test Bed Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Academic Test Bed Stream", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "Emulate calibrated sensor telemetry payload for demonstration purposes.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = sensorState.isAcademicTestBedActive,
                        onCheckedChange = { viewModel.toggleAcademicSensorTestBed(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldPrimary.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Active Telemetry Readings (if testbed is active)
        if (sensorState.latestReadings.isNotEmpty()) {
            item {
                Text("Incoming Sensor Telemetry Packet:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pm25 = sensorState.latestReadings["PM2.5"] ?: 0.0
                    EnvironmentalKpiCard(
                        title = "PM2.5 Sensor",
                        value = String.format("%.1f", pm25),
                        unit = "µg/m³",
                        statusText = "Calibrated",
                        statusColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    val pm10 = sensorState.latestReadings["PM10"] ?: 0.0
                    EnvironmentalKpiCard(
                        title = "PM10 Sensor",
                        value = String.format("%.1f", pm10),
                        unit = "µg/m³",
                        statusText = "Calibrated",
                        statusColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val no2 = sensorState.latestReadings["NO2"] ?: 0.0
                    EnvironmentalKpiCard(
                        title = "NO2 Electrochemical",
                        value = String.format("%.1f", no2),
                        unit = "µg/m³",
                        statusText = "Normal",
                        statusColor = CyanSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    val temp = sensorState.latestReadings["Temperature"] ?: 0.0
                    EnvironmentalKpiCard(
                        title = "DHT22 Temperature",
                        value = String.format("%.1f", temp),
                        unit = "°C",
                        statusText = "Ambient",
                        statusColor = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // REST API Ingestion Endpoint Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("API Sensor Ingestion Endpoint", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpointUrl,
                        onValueChange = { endpointUrl = it },
                        label = { Text("Gateway Endpoint URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("IoT Node Authentication Bearer Token") },
                        placeholder = { Text("sec_live_aerocast_******************") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Payload format expected by backend: JSON with { timestamp, sensor_id, pm2_5, pm10, no2, so2, co, o3, temp, humidity }.",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
