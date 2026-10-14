package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ml.AcademicBenchmarkDataset
import com.example.ui.AirQualityViewModel
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import androidx.compose.material3.HorizontalDivider
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun DatasetScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val dataset by viewModel.latestDataset.collectAsState()
    val parsed by viewModel.parsedDataset.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var showCustomCsvDialog by remember { mutableStateOf(false) }
    var customCsvInput by remember { mutableStateOf("") }
    var customFileName by remember { mutableStateOf("custom_air_quality.csv") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Dataset Management", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Upload and inspect historical atmospheric observations",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.loadBenchmarkDataset() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load Benchmark CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showCustomCsvDialog = !showCustomCsvDialog },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom CSV", fontSize = 11.sp, color = CyanSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Custom CSV Input Area
        if (showCustomCsvDialog) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Paste Custom CSV Data", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customFileName,
                            onValueChange = { customFileName = it },
                            label = { Text("File Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customCsvInput,
                            onValueChange = { customCsvInput = it },
                            label = { Text("CSV Text (Headers + Rows)") },
                            placeholder = { Text("Timestamp,PM2.5,PM10,NO2,Temp,Humidity\n2026-10-01 01:00,45.2,85.1,32.0,24.1,65.0") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (customCsvInput.isNotBlank()) {
                                        viewModel.uploadCustomCsv(customFileName, customCsvInput)
                                        showCustomCsvDialog = false
                                        customCsvInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Parse & Ingest CSV", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { showCustomCsvDialog = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Active Dataset Card Summary
        item {
            if (parsed != null && dataset != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(dataset!!.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Ready for ML",
                                    color = EmeraldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Rows Count", color = TextSecondary, fontSize = 10.sp)
                                Text("${parsed!!.rows.size}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Columns Count", color = TextSecondary, fontSize = 10.sp)
                                Text("${parsed!!.headers.size}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Timestamp Col", color = TextSecondary, fontSize = 10.sp)
                                Text(parsed!!.detectedTimestampColumn ?: "None", color = CyanSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Target Candidate", color = TextSecondary, fontSize = 10.sp)
                                Text(parsed!!.detectedTargetCandidate, color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Column Aliases detected
                        Text("Detected Parameter Aliases:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            parsed!!.detectedAliases.forEach { (raw, standard) ->
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text(
                                        text = "$raw → $standard",
                                        color = CyanSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigate(NavScreen.PREPROCESSING) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Proceed to Preprocessing Pipeline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Dataset, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active dataset loaded.", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Tap 'Load Benchmark CSV' to start with pre-generated 120-hour environmental records.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Preview Table
        if (parsed != null && parsed!!.rows.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dataset Preview (First 8 Rows)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${parsed!!.rows.size} Total Rows", color = TextSecondary, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.horizontalScroll(scrollState)) {
                        // Headers
                        Row(modifier = Modifier.background(DarkSurfaceElevated, RoundedCornerShape(6.dp)).padding(vertical = 6.dp)) {
                            parsed!!.headers.forEach { h ->
                                Text(
                                    text = h,
                                    color = if (h == parsed!!.detectedTargetCandidate) EmeraldPrimary else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(100.dp).padding(horizontal = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                        // Rows
                        parsed!!.rows.take(8).forEachIndexed { rIdx, row ->
                            Row(
                                modifier = Modifier
                                    .background(if (rIdx % 2 == 0) DarkSurfaceVariant else DarkSurface)
                                    .padding(vertical = 5.dp)
                            ) {
                                row.forEachIndexed { cIdx, cell ->
                                    Text(
                                        text = cell,
                                        color = if (cIdx == parsed!!.headers.indexOf(parsed!!.detectedTargetCandidate)) EmeraldPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(100.dp).padding(horizontal = 6.dp)
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
