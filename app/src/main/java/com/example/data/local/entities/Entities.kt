package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rowCount: Int,
    val columnCount: Int,
    val columnsJson: String,
    val uploadedAt: Long,
    val targetColumn: String = "PM2.5",
    val isCleaned: Boolean = true
)

@Entity(tableName = "trained_models")
data class TrainedModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val algorithm: String, // "Random Forest", "Decision Tree", "K-Nearest Neighbors", "XGBoost", "1D CNN Time-Series"
    val targetPollutant: String,
    val mae: Double,
    val mse: Double,
    val rmse: Double,
    val r2: Double,
    val trainTimeMs: Long,
    val testSampleSize: Int,
    val trainedAt: Long,
    val hyperparamsJson: String = "{}",
    val isBestModel: Boolean = false
)

@Entity(tableName = "prediction_history")
data class PredictionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modelName: String,
    val targetPollutant: String,
    val predictedValue: Double,
    val horizonHours: Int,
    val generatedAt: Long,
    val aqiCategory: String,
    val aqiValue: Int,
    val inputDetails: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pollutant: String,
    val concentration: Double,
    val threshold: Double,
    val severity: String, // "WARNING", "CRITICAL"
    val status: String,   // "ACTIVE", "ACKNOWLEDGED", "RESOLVED"
    val message: String,
    val timestamp: Long
)

@Entity(tableName = "alert_thresholds")
data class AlertThresholdEntity(
    @PrimaryKey val pollutant: String,
    val warningLimit: Double,
    val criticalLimit: Double,
    val isEnabled: Boolean = true
)

@Entity(tableName = "sensor_logs")
data class SensorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sensorId: String,
    val pollutant: String,
    val value: Double,
    val timestamp: Long,
    val isLiveSource: Boolean
)
