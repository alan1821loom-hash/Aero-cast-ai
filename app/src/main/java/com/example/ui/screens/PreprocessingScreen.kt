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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.ImputationStrategy
import com.example.ui.AirQualityViewModel
import com.example.ui.navigation.NavScreen
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LimeTertiary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PreprocessingScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed by viewModel.parsedDataset.collectAsState()
    val audit by viewModel.auditSummary.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var selectedTarget by remember(parsed) {
        mutableStateOf(parsed?.detectedTargetCandidate ?: "PM2.5")
    }

    var selectedImputation by remember {
        mutableStateOf(ImputationStrategy.FORWARD_FILL)
    }

    val selectedFeatures = remember(parsed, selectedTarget) {
        val initial = parsed?.headers?.filter { h ->
            h != parsed?.detectedTimestampColumn && h != selectedTarget
        } ?: emptyList()
        mutableStateListOf<String>().apply { addAll(initial) }
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
                Text("Data Preprocessing Pipeline", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Rigorous transformation, chronological sorting, and data leakage prevention",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Target Variable Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("1. Select Target Prediction Variable", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("The numerical environmental parameter to predict (e.g., PM2.5 concentration):", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val candidateTargets = parsed?.headers?.filter { it != parsed?.detectedTimestampColumn }
                        ?: listOf("PM2.5", "PM10", "NO2", "CO")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        candidateTargets.forEach { target ->
                            val isSelected = target == selectedTarget
                            Surface(
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) EmeraldPrimary else DarkBorder
                                ),
                                modifier = Modifier.clickable {
                                    selectedTarget = target
                                    selectedFeatures.remove(target)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = target,
                                        color = if (isSelected) EmeraldPrimary else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("2. Feature Selection (${selectedFeatures.size} Selected)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose atmospheric & meteorological inputs used by regression models:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val availableFeatures = parsed?.headers?.filter {
                        it != parsed?.detectedTimestampColumn && it != selectedTarget
                    } ?: emptyList()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableFeatures.forEach { feat ->
                            val isChecked = selectedFeatures.contains(feat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isChecked) {
                                            if (selectedFeatures.size > 1) selectedFeatures.remove(feat)
                                        } else {
                                            selectedFeatures.add(feat)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { check ->
                                        if (check) selectedFeatures.add(feat)
                                        else if (selectedFeatures.size > 1) selectedFeatures.remove(feat)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CyanSecondary,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(feat, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Missing-Value Imputation Strategy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("3. Missing-Value Imputation Strategy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Time-series data requires Forward-Fill to prevent future information leakage:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    ImputationStrategy.values().forEach { strategy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedImputation = strategy }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedImputation == strategy,
                                onClick = { selectedImputation = strategy },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strategy.label, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Apply Transformation Button
        item {
            Button(
                onClick = {
                    viewModel.preprocessDataset(
                        target = selectedTarget,
                        features = selectedFeatures.toList(),
                        imputation = selectedImputation
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                enabled = !isProcessing && selectedFeatures.isNotEmpty()
            ) {
                Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Execute Preprocessing Pipeline", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Preprocessing Audit Card (if available)
        if (audit != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = LimeTertiary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Data Leakage Prevention Audit", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "PASSED",
                                    color = EmeraldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Original Rows", color = TextSecondary, fontSize = 10.sp)
                                Text("${audit!!.originalRowCount}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Cleaned Rows", color = TextSecondary, fontSize = 10.sp)
                                Text("${audit!!.cleanedRowCount}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Train Samples (80%)", color = TextSecondary, fontSize = 10.sp)
                                Text("${audit!!.trainCount}", color = CyanSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Test Samples (20%)", color = TextSecondary, fontSize = 10.sp)
                                Text("${audit!!.testCount}", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Verification Details:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Chronological train/test partition applied without random shuffling.", color = TextTertiary, fontSize = 10.sp)
                        Text("• StandardScaler z-scores fitted strictly on trainX (no future contamination).", color = TextTertiary, fontSize = 10.sp)
                        Text("• Target variable: ${audit!!.targetVariable} | Features: ${audit!!.selectedFeatures.size}", color = TextTertiary, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigate(NavScreen.MODELS) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Proceed to Model Training Studio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
