package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AlertEntity
import com.example.data.local.entities.AlertThresholdEntity
import com.example.data.local.entities.DatasetMetadataEntity
import com.example.data.local.entities.PredictionHistoryEntity
import com.example.data.local.entities.SensorLogEntity
import com.example.data.local.entities.TrainedModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AirQualityDao {

    // Datasets
    @Query("SELECT * FROM dataset_metadata ORDER BY uploadedAt DESC")
    fun getAllDatasets(): Flow<List<DatasetMetadataEntity>>

    @Query("SELECT * FROM dataset_metadata ORDER BY uploadedAt DESC LIMIT 1")
    fun getLatestDataset(): Flow<DatasetMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: DatasetMetadataEntity): Long

    @Query("DELETE FROM dataset_metadata")
    suspend fun clearDatasets()

    // Trained Models
    @Query("SELECT * FROM trained_models ORDER BY trainedAt DESC")
    fun getAllModels(): Flow<List<TrainedModelEntity>>

    @Query("SELECT * FROM trained_models WHERE targetPollutant = :target ORDER BY r2 DESC")
    fun getModelsByTarget(target: String): Flow<List<TrainedModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: TrainedModelEntity): Long

    @Query("UPDATE trained_models SET isBestModel = 0 WHERE targetPollutant = :target")
    suspend fun resetBestModelFlags(target: String)

    @Query("UPDATE trained_models SET isBestModel = 1 WHERE id = :modelId")
    suspend fun markBestModel(modelId: Int)

    @Query("DELETE FROM trained_models")
    suspend fun clearModels()

    // Prediction History
    @Query("SELECT * FROM prediction_history ORDER BY generatedAt DESC")
    fun getPredictionHistory(): Flow<List<PredictionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionHistoryEntity): Long

    @Query("DELETE FROM prediction_history")
    suspend fun clearPredictions()

    // Alerts
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET status = 'ACKNOWLEDGED' WHERE id = :alertId")
    suspend fun acknowledgeAlert(alertId: Int)

    @Query("UPDATE alerts SET status = 'RESOLVED' WHERE id = :alertId")
    suspend fun resolveAlert(alertId: Int)

    @Query("DELETE FROM alerts")
    suspend fun clearAlerts()

    // Alert Thresholds
    @Query("SELECT * FROM alert_thresholds")
    fun getAllThresholds(): Flow<List<AlertThresholdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreshold(threshold: AlertThresholdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThresholds(thresholds: List<AlertThresholdEntity>)

    // Sensor Logs
    @Query("SELECT * FROM sensor_logs ORDER BY timestamp DESC LIMIT 50")
    fun getLatestSensorLogs(): Flow<List<SensorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorLog(log: SensorLogEntity): Long
}
