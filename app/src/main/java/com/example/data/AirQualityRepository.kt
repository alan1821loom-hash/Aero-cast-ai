package com.example.data

import com.example.data.local.dao.AirQualityDao
import com.example.data.local.entities.AlertEntity
import com.example.data.local.entities.AlertThresholdEntity
import com.example.data.local.entities.DatasetMetadataEntity
import com.example.data.local.entities.PredictionHistoryEntity
import com.example.data.local.entities.SensorLogEntity
import com.example.data.local.entities.TrainedModelEntity
import com.example.ml.AirQualityRegressor
import com.example.ml.DatasetSplit
import com.example.ml.ParsedDataset
import com.example.ml.PreprocessingAudit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AirQualityRepository(private val dao: AirQualityDao) {

    // In-memory runtime state for active session data & models
    var activeParsedDataset: ParsedDataset? = null
    var activeSplit: DatasetSplit? = null
    var activeAudit: PreprocessingAudit? = null
    val trainedModelsMap = mutableMapOf<String, AirQualityRegressor>()

    val allDatasets: Flow<List<DatasetMetadataEntity>> = dao.getAllDatasets()
    val latestDataset: Flow<DatasetMetadataEntity?> = dao.getLatestDataset()
    val allModels: Flow<List<TrainedModelEntity>> = dao.getAllModels()
    val predictionHistory: Flow<List<PredictionHistoryEntity>> = dao.getPredictionHistory()
    val allAlerts: Flow<List<AlertEntity>> = dao.getAllAlerts()
    val activeAlerts: Flow<List<AlertEntity>> = dao.getActiveAlerts()
    val allThresholds: Flow<List<AlertThresholdEntity>> = dao.getAllThresholds()
    val sensorLogs: Flow<List<SensorLogEntity>> = dao.getLatestSensorLogs()

    suspend fun saveDataset(entity: DatasetMetadataEntity): Long = dao.insertDataset(entity)

    suspend fun clearDatasets() = dao.clearDatasets()

    suspend fun saveModel(model: TrainedModelEntity): Long {
        return dao.insertModel(model)
    }

    suspend fun markBestModel(modelId: Int, target: String) {
        dao.resetBestModelFlags(target)
        dao.markBestModel(modelId)
    }

    suspend fun clearModels() {
        trainedModelsMap.clear()
        dao.clearModels()
    }

    suspend fun savePrediction(prediction: PredictionHistoryEntity): Long = dao.insertPrediction(prediction)

    suspend fun clearPredictions() = dao.clearPredictions()

    suspend fun saveAlert(alert: AlertEntity): Long = dao.insertAlert(alert)

    suspend fun acknowledgeAlert(alertId: Int) = dao.acknowledgeAlert(alertId)

    suspend fun resolveAlert(alertId: Int) = dao.resolveAlert(alertId)

    suspend fun saveThreshold(threshold: AlertThresholdEntity) = dao.insertThreshold(threshold)

    suspend fun saveSensorLog(log: SensorLogEntity): Long = dao.insertSensorLog(log)

    suspend fun seedDefaultThresholdsIfEmpty() {
        val existing = dao.getAllThresholds().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                AlertThresholdEntity("PM2.5", warningLimit = 60.0, criticalLimit = 120.0),
                AlertThresholdEntity("PM10", warningLimit = 100.0, criticalLimit = 250.0),
                AlertThresholdEntity("NO2", warningLimit = 80.0, criticalLimit = 180.0),
                AlertThresholdEntity("SO2", warningLimit = 80.0, criticalLimit = 380.0),
                AlertThresholdEntity("CO", warningLimit = 2.0, criticalLimit = 10.0),
                AlertThresholdEntity("O3", warningLimit = 100.0, criticalLimit = 168.0)
            )
            dao.insertThresholds(defaults)
        }
    }
}
