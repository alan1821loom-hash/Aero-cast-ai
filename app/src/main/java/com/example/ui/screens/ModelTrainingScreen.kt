package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.ModelAlgorithm
import com.example.ui.AirQualityViewModel
import com.example.ui.components.EnvironmentalLineChart
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
fun ModelTrainingScreen(
    viewModel: AirQualityViewModel,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val audit by viewModel.auditSummary.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val trainingStatus by viewModel.trainingStatus.collectAsState()
    val trainedModels by viewModel.allModels.collectAsState()
    val cnnLosses by viewModel.cnnLossHistory.collectAsState()
    val lstmLosses by viewModel.lstmLossHistory.collectAsState()

    var selectedAlgo by remember { mutableStateOf(ModelAlgorithm.RANDOM_FOREST) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Model Training Studio", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Train Machine Learning & Deep Learning architectures on preprocessed observations",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Training Status banner if running
        if (isProcessing) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, EmeraldPrimary, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = trainingStatus,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Buttons: Train Selected & Train All
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.trainModel(selectedAlgo) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing && audit != null
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Train Selected", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.trainAllModels() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isProcessing && audit != null
                ) {
                    Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Train All 6 Models", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Algorithm Selection Cards
        item {
            Text("Supported Machine Learning & Deep Learning Models:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        items(ModelAlgorithm.values().size) { idx ->
            val algo = ModelAlgorithm.values()[idx]
            val isSelected = algo == selectedAlgo
            val trainedRecord = trainedModels.firstOrNull { it.algorithm == algo.name }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedAlgo = algo },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceElevated else DarkSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) CyanSecondary else DarkBorder
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (isSelected) CyanSecondary else TextTertiary,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    algo.displayName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(algo.category, color = TextSecondary, fontSize = 11.sp)
                        }

                        if (trainedRecord != null) {
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "R² = ${String.format("%.3f", trainedRecord.r2)}",
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brief description
                    val desc = when (algo) {
                        ModelAlgorithm.RANDOM_FOREST -> "Ensemble of randomized decision trees with bootstrap aggregation and feature sub-sampling to lower forecast variance."
                        ModelAlgorithm.DECISION_TREE -> "Non-parametric greedy recursive split optimization minimizing Mean Squared Error (variance reduction)."
                        ModelAlgorithm.KNN -> "Instance-based regression computing Euclidean distance across standardized environmental dimensions."
                        ModelAlgorithm.XGBOOST -> "Iterative gradient boosting fitting weak regression trees to residual gradients with shrinkage regularization."
                        ModelAlgorithm.CNN_1D -> "1D Convolutional Neural Network sliding temporal filters over sequential windows to extract temporal atmospheric dynamics."
                        ModelAlgorithm.LSTM_TIME_SERIES -> "Long Short-Term Memory recurrent neural network capturing persistent sequential dependencies via input, forget, and output gating mechanisms."
                    }
                    Text(desc, color = TextTertiary, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }

        // CNN 1D Deep Learning Training History (Epoch Loss curve)
        if (cnnLosses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = LimeTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1D CNN Training Convergence", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("${cnnLosses.size} Epochs", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    EnvironmentalLineChart(
                        actualData = cnnLosses,
                        title = "Mean Squared Error Loss vs Epochs",
                        unit = "MSE",
                        primaryColor = LimeTertiary
                    )
                }
            }
        }

        // LSTM Deep Learning Training History (Epoch Loss curve)
        if (lstmLosses.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LSTM Recurrent Neural Network Convergence", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("${lstmLosses.size} Epochs", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    EnvironmentalLineChart(
                        actualData = lstmLosses,
                        title = "BPTT Loss vs Training Epochs",
                        unit = "MSE",
                        primaryColor = CyanSecondary
                    )
                }
            }
        }

        // Navigation to Model Evaluation
        if (trainedModels.isNotEmpty()) {
            item {
                Button(
                    onClick = { onNavigate(NavScreen.EVALUATION) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Model Evaluation & Comparison", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
