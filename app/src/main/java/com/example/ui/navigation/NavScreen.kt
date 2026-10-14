package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavScreen(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    DASHBOARD("Dashboard", Icons.Default.CloudQueue, "System overview & AQI metrics"),
    DATASET("Datasets", Icons.Default.Dataset, "CSV ingestion & preview"),
    PREPROCESSING("Preprocessing", Icons.Default.Engineering, "Cleansing & train/test split"),
    MODELS("ML Training", Icons.Default.ModelTraining, "Train regression & CNN models"),
    EVALUATION("Evaluation", Icons.Default.Assessment, "Model comparison & metrics"),
    PREDICTION("Prediction", Icons.Default.AutoGraph, "Multi-horizon forecasting"),
    RISK_AQI("AQI Standards", Icons.Default.Analytics, "CPCB & EPA index calculation"),
    SENSOR("IoT Sensors", Icons.Default.Sensors, "Live & academic sensor stream"),
    ALERTS("Alerts", Icons.Default.NotificationsActive, "Threshold breach monitoring"),
    REPORTS("Reports & Project", Icons.Default.Info, "Academic project & summary")
}
