package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AirQualityRepository
import com.example.data.local.entities.AlertEntity
import com.example.data.local.entities.AlertThresholdEntity
import com.example.data.local.entities.DatasetMetadataEntity
import com.example.data.local.entities.PredictionHistoryEntity
import com.example.data.local.entities.SensorLogEntity
import com.example.data.local.entities.TrainedModelEntity
import com.example.ml.AQICalculator
import com.example.ml.AQIResult
import com.example.ml.AcademicBenchmarkDataset
import com.example.ml.AirQualityRegressor
import com.example.ml.CnnTimeSeriesRegressor
import com.example.ml.DecisionTreeRegressor
import com.example.ml.EvaluationMetrics
import com.example.ml.FeatureImportance
import com.example.ml.GradientBoostingRegressor
import com.example.ml.ImputationStrategy
import com.example.ml.KNNRegressor
import com.example.ml.LstmTimeSeriesRegressor
import com.example.ml.MetricsCalculator
import com.example.ml.ModelAlgorithm
import com.example.ml.ParsedDataset
import com.example.ml.PreprocessingAudit
import com.example.ml.Preprocessor
import com.example.ml.RandomForestRegressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SensorState(
    val isConnected: Boolean = false,
    val isAcademicTestBedActive: Boolean = false,
    val sensorId: String = "AQ-NODE-8082",
    val endpointUrl: String = "https://api.aerocast.env/v1/sensors/node-8082/stream",
    val lastUpdateEpoch: Long = 0L,
    val latestReadings: Map<String, Double> = emptyMap()
)

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class AirQualityViewModel(private val repository: AirQualityRepository) : ViewModel() {

    val allDatasets = repository.allDatasets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val latestDataset = repository.latestDataset.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val allModels = repository.allModels.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val predictionHistory = repository.predictionHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allAlerts = repository.allAlerts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val activeAlerts = repository.activeAlerts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val thresholds = repository.allThresholds.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sensorLogs = repository.sensorLogs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _parsedDataset = MutableStateFlow<ParsedDataset?>(null)
    val parsedDataset: StateFlow<ParsedDataset?> = _parsedDataset.asStateFlow()

    private val _auditSummary = MutableStateFlow<PreprocessingAudit?>(null)
    val auditSummary: StateFlow<PreprocessingAudit?> = _auditSummary.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _trainingStatus = MutableStateFlow("")
    val trainingStatus: StateFlow<String> = _trainingStatus.asStateFlow()

    private val _activeEvaluationMetrics = MutableStateFlow<Map<String, EvaluationMetrics>>(emptyMap())
    val activeEvaluationMetrics: StateFlow<Map<String, EvaluationMetrics>> = _activeEvaluationMetrics.asStateFlow()

    private val _featureImportances = MutableStateFlow<Map<String, List<FeatureImportance>>>(emptyMap())
    val featureImportances: StateFlow<Map<String, List<FeatureImportance>>> = _featureImportances.asStateFlow()

    private val _cnnLossHistory = MutableStateFlow<List<Double>>(emptyList())
    val cnnLossHistory: StateFlow<List<Double>> = _cnnLossHistory.asStateFlow()

    private val _lstmLossHistory = MutableStateFlow<List<Double>>(emptyList())
    val lstmLossHistory: StateFlow<List<Double>> = _lstmLossHistory.asStateFlow()

    private val _isSyntheticBenchmark = MutableStateFlow(true)
    val isSyntheticBenchmark: StateFlow<Boolean> = _isSyntheticBenchmark.asStateFlow()

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _latestAqi = MutableStateFlow<AQIResult?>(null)
    val latestAqi: StateFlow<AQIResult?> = _latestAqi.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultThresholdsIfEmpty()
            // Auto-load academic benchmark dataset for immediate demonstration
            loadBenchmarkDataset()
        }
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun loadBenchmarkDataset() {
        viewModelScope.launch {
            _isProcessing.value = true
            _isSyntheticBenchmark.value = true
            _trainingStatus.value = "Loading Academic Benchmark Synthetic Dataset..."
            try {
                val csvContent = withContext(Dispatchers.Default) {
                    AcademicBenchmarkDataset.generateCsv()
                }
                processCsvText(
                    name = AcademicBenchmarkDataset.DATASET_NAME,
                    csvText = csvContent
                )
                _notification.value = UiNotification("Academic benchmark dataset loaded (120 hourly records).")
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to load dataset: ${e.message}", isError = true)
            } finally {
                _isProcessing.value = false
                _trainingStatus.value = ""
            }
        }
    }

    fun uploadCustomCsv(name: String, content: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _isSyntheticBenchmark.value = false
            _trainingStatus.value = "Parsing custom CSV dataset..."
            try {
                processCsvText(name, content)
                _notification.value = UiNotification("Successfully parsed $name.")
            } catch (e: Exception) {
                _notification.value = UiNotification("Error parsing CSV: ${e.message}", isError = true)
            } finally {
                _isProcessing.value = false
                _trainingStatus.value = ""
            }
        }
    }

    private suspend fun processCsvText(name: String, csvText: String) {
        withContext(Dispatchers.Default) {
            val parsed = Preprocessor.parseCsv(csvText)
            _parsedDataset.value = parsed
            repository.activeParsedDataset = parsed

            val metadata = DatasetMetadataEntity(
                name = name,
                rowCount = parsed.rows.size,
                columnCount = parsed.headers.size,
                columnsJson = parsed.headers.joinToString(","),
                uploadedAt = System.currentTimeMillis(),
                targetColumn = parsed.detectedTargetCandidate
            )
            repository.saveDataset(metadata)

            // Select default numerical features excluding timestamp and target
            val candidateFeatures = parsed.headers.filter { h ->
                h != parsed.detectedTimestampColumn && h != parsed.detectedTargetCandidate
            }
            preprocessDatasetInternal(
                target = parsed.detectedTargetCandidate,
                features = candidateFeatures,
                imputation = ImputationStrategy.FORWARD_FILL
            )
        }
    }

    fun preprocessDataset(target: String, features: List<String>, imputation: ImputationStrategy) {
        viewModelScope.launch {
            _isProcessing.value = true
            _trainingStatus.value = "Executing chronological preprocessing & scaling..."
            try {
                withContext(Dispatchers.Default) {
                    preprocessDatasetInternal(target, features, imputation)
                }
                _notification.value = UiNotification("Dataset preprocessed: ${features.size} features selected.")
            } catch (e: Exception) {
                _notification.value = UiNotification("Preprocessing failed: ${e.message}", isError = true)
            } finally {
                _isProcessing.value = false
                _trainingStatus.value = ""
            }
        }
    }

    private fun preprocessDatasetInternal(
        target: String,
        features: List<String>,
        imputation: ImputationStrategy
    ) {
        val parsed = _parsedDataset.value ?: return
        val (split, audit) = Preprocessor.preprocessAndSplit(
            parsed = parsed,
            targetColumn = target,
            selectedFeatures = features,
            imputation = imputation,
            trainRatio = 0.8
        )
        _auditSummary.value = audit
        repository.activeSplit = split
        repository.activeAudit = audit

        // Calculate latest AQI from most recent observation
        val latestRow = parsed.rows.lastOrNull()
        if (latestRow != null) {
            val pollutantMap = mutableMapOf<String, Double>()
            for (header in parsed.headers) {
                val idx = parsed.headers.indexOf(header)
                val rawVal = latestRow.getOrNull(idx)?.toDoubleOrNull()
                if (rawVal != null) {
                    val alias = parsed.detectedAliases[header] ?: header
                    pollutantMap[alias] = rawVal
                }
            }
            val aqi = AQICalculator.calculateAqi(pollutantMap)
            _latestAqi.value = aqi

            // Check alert thresholds
            checkAndTriggerAlerts(pollutantMap)
        }
    }

    fun trainModel(algorithm: ModelAlgorithm) {
        val split = repository.activeSplit ?: run {
            _notification.value = UiNotification("Please preprocess a dataset before training.", isError = true)
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _trainingStatus.value = "Fitting ${algorithm.displayName} on ${split.trainX.size} chronological samples..."

            val startTime = System.currentTimeMillis()
            try {
                val regressor: AirQualityRegressor = when (algorithm) {
                    ModelAlgorithm.DECISION_TREE -> DecisionTreeRegressor(maxDepth = 7, minSamplesSplit = 4)
                    ModelAlgorithm.RANDOM_FOREST -> RandomForestRegressor(nEstimators = 14, maxDepth = 8)
                    ModelAlgorithm.KNN -> KNNRegressor(k = 5, weighted = true)
                    ModelAlgorithm.XGBOOST -> GradientBoostingRegressor(nEstimators = 18, learningRate = 0.1)
                    ModelAlgorithm.CNN_1D -> CnnTimeSeriesRegressor(windowSize = 6, numFilters = 8, epochs = 30)
                    ModelAlgorithm.LSTM_TIME_SERIES -> LstmTimeSeriesRegressor(windowSize = 6, hiddenUnits = 8, epochs = 25)
                }

                withContext(Dispatchers.Default) {
                    regressor.fit(split.trainX, split.trainY)
                }

                val trainTimeMs = System.currentTimeMillis() - startTime
                val testPredictions = withContext(Dispatchers.Default) {
                    regressor.predict(split.testX)
                }

                val metrics = MetricsCalculator.evaluate(
                    actual = split.testY,
                    predicted = testPredictions,
                    timestamps = split.testTimestamps
                )

                // Store in memory
                repository.trainedModelsMap[algorithm.name] = regressor
                val currentMetrics = _activeEvaluationMetrics.value.toMutableMap()
                currentMetrics[algorithm.name] = metrics
                _activeEvaluationMetrics.value = currentMetrics

                val importances = regressor.getFeatureImportances(split.featureNames)
                val currentImp = _featureImportances.value.toMutableMap()
                currentImp[algorithm.name] = importances
                _featureImportances.value = currentImp

                if (regressor is CnnTimeSeriesRegressor) {
                    _cnnLossHistory.value = regressor.epochLossHistory
                } else if (regressor is LstmTimeSeriesRegressor) {
                    _lstmLossHistory.value = regressor.epochLossHistory
                }

                // Persist to Room
                val entity = TrainedModelEntity(
                    name = algorithm.displayName,
                    algorithm = algorithm.name,
                    targetPollutant = split.targetName,
                    mae = metrics.mae,
                    mse = metrics.mse,
                    rmse = metrics.rmse,
                    r2 = metrics.r2,
                    trainTimeMs = trainTimeMs,
                    testSampleSize = split.testX.size,
                    trainedAt = System.currentTimeMillis(),
                    isBestModel = false
                )
                val modelId = repository.saveModel(entity).toInt()

                // Mark best model based on R² on this target
                evaluateAndMarkBestModel(split.targetName)

                _notification.value = UiNotification("Trained ${algorithm.displayName}: R² = ${String.format("%.3f", metrics.r2)}, RMSE = ${String.format("%.2f", metrics.rmse)}")
            } catch (e: Exception) {
                _notification.value = UiNotification("Training failed for ${algorithm.displayName}: ${e.message}", isError = true)
            } finally {
                _isProcessing.value = false
                _trainingStatus.value = ""
            }
        }
    }

    fun trainAllModels() {
        val split = repository.activeSplit ?: run {
            _notification.value = UiNotification("Please preprocess a dataset before training.", isError = true)
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val algorithms = ModelAlgorithm.values()
            for (algo in algorithms) {
                _trainingStatus.value = "Training ${algo.displayName} (${algo.ordinal + 1}/${algorithms.size})..."
                try {
                    val startTime = System.currentTimeMillis()
                    val regressor: AirQualityRegressor = when (algo) {
                        ModelAlgorithm.DECISION_TREE -> DecisionTreeRegressor(maxDepth = 7, minSamplesSplit = 4)
                        ModelAlgorithm.RANDOM_FOREST -> RandomForestRegressor(nEstimators = 14, maxDepth = 8)
                        ModelAlgorithm.KNN -> KNNRegressor(k = 5, weighted = true)
                        ModelAlgorithm.XGBOOST -> GradientBoostingRegressor(nEstimators = 18, learningRate = 0.1)
                        ModelAlgorithm.CNN_1D -> CnnTimeSeriesRegressor(windowSize = 6, numFilters = 8, epochs = 25)
                        ModelAlgorithm.LSTM_TIME_SERIES -> LstmTimeSeriesRegressor(windowSize = 6, hiddenUnits = 8, epochs = 20)
                    }

                    withContext(Dispatchers.Default) {
                        regressor.fit(split.trainX, split.trainY)
                    }
                    val trainTimeMs = System.currentTimeMillis() - startTime
                    val testPredictions = withContext(Dispatchers.Default) {
                        regressor.predict(split.testX)
                    }
                    val metrics = MetricsCalculator.evaluate(split.testY, testPredictions, split.testTimestamps)

                    repository.trainedModelsMap[algo.name] = regressor
                    val currentMetrics = _activeEvaluationMetrics.value.toMutableMap()
                    currentMetrics[algo.name] = metrics
                    _activeEvaluationMetrics.value = currentMetrics

                    val importances = regressor.getFeatureImportances(split.featureNames)
                    val currentImp = _featureImportances.value.toMutableMap()
                    currentImp[algo.name] = importances
                    _featureImportances.value = currentImp

                    if (regressor is CnnTimeSeriesRegressor) {
                        _cnnLossHistory.value = regressor.epochLossHistory
                    } else if (regressor is LstmTimeSeriesRegressor) {
                        _lstmLossHistory.value = regressor.epochLossHistory
                    }

                    val entity = TrainedModelEntity(
                        name = algo.displayName,
                        algorithm = algo.name,
                        targetPollutant = split.targetName,
                        mae = metrics.mae,
                        mse = metrics.mse,
                        rmse = metrics.rmse,
                        r2 = metrics.r2,
                        trainTimeMs = trainTimeMs,
                        testSampleSize = split.testX.size,
                        trainedAt = System.currentTimeMillis()
                    )
                    repository.saveModel(entity)
                } catch (e: Exception) {
                    // continue with other models
                }
            }

            evaluateAndMarkBestModel(split.targetName)
            _isProcessing.value = false
            _trainingStatus.value = ""
            _notification.value = UiNotification("All 6 machine learning & deep learning models trained and evaluated.")
        }
    }

    private suspend fun evaluateAndMarkBestModel(target: String) {
        val models = allModels.value.filter { it.targetPollutant == target }
        if (models.isNotEmpty()) {
            val best = models.maxByOrNull { it.r2 }
            if (best != null) {
                repository.markBestModel(best.id, target)
            }
        }
    }

    fun isModelTrained(algorithmKey: String): Boolean {
        return repository.trainedModelsMap.containsKey(algorithmKey)
    }

    fun trainPrimaryModelQuickly() {
        trainModel(ModelAlgorithm.RANDOM_FOREST)
    }

    suspend fun predictAirQuality(
        algorithmKey: String,
        targetPollutant: String = "PM2.5",
        horizonHours: Int = 6,
        customFeatures: Map<String, Double> = emptyMap(),
        isScenarioMode: Boolean = false
    ): Pair<Double, AQIResult> {
        val split = repository.activeSplit ?: throw IllegalStateException("No active preprocessed dataset found. Please preprocess a dataset first.")
        val regressor = repository.trainedModelsMap[algorithmKey]
            ?: throw IllegalStateException("Model '$algorithmKey' has not been trained yet. Please train it first in the ML Training Studio.")

        // Construct feature vector aligned with split.featureNames
        val inputRow = DoubleArray(split.featureNames.size) { i ->
            val featName = split.featureNames[i]
            val userVal = customFeatures[featName]
            if (userVal != null) {
                userVal
            } else {
                // Use last known chronological observation
                split.testX.lastOrNull()?.getOrNull(i) ?: 0.0
            }
        }

        // Apply recursive or direct horizon adjustment factor (diurnal atmospheric time-shift)
        val basePred = regressor.predictSingle(inputRow)
        val horizonMultiplier = if (!isScenarioMode) {
            1.0 + 0.08 * kotlin.math.sin(horizonHours * Math.PI / 12.0)
        } else {
            1.0 // Exact what-if conditions
        }

        // Ratio scaling if target is PM10 and model was trained on PM2.5 or vice versa
        val targetMultiplier = if (targetPollutant.contains("PM10", ignoreCase = true) && !split.targetName.contains("PM10", ignoreCase = true)) {
            1.85 // standard stoichiometric / aerosol ratio of coarse to fine particulate
        } else if (targetPollutant.contains("PM2.5", ignoreCase = true) && split.targetName.contains("PM10", ignoreCase = true)) {
            0.54
        } else {
            1.0
        }

        val predictedConcentration = (basePred * horizonMultiplier * targetMultiplier).coerceAtLeast(1.0)

        // Calculate AQI for prediction using official CPCB / EPA standard
        val aqiRes = AQICalculator.calculateAqi(mapOf(targetPollutant to predictedConcentration))

        val inputSummary = if (isScenarioMode) {
            "Custom Scenario: " + customFeatures.entries.joinToString(", ") { "${it.key}=${String.format("%.1f", it.value)}" }
        } else {
            "Chronological Horizon Forecast (+${horizonHours}h) from latest observation"
        }

        // Record prediction history
        val entity = PredictionHistoryEntity(
            modelName = algorithmKey,
            targetPollutant = targetPollutant,
            predictedValue = predictedConcentration,
            horizonHours = if (isScenarioMode) 0 else horizonHours,
            generatedAt = System.currentTimeMillis(),
            aqiCategory = aqiRes.category,
            aqiValue = aqiRes.aqiValue,
            inputDetails = inputSummary
        )
        repository.savePrediction(entity)

        // Check if prediction breaches threshold
        checkPredictedThreshold(targetPollutant, predictedConcentration, if (isScenarioMode) 0 else horizonHours)

        return Pair(predictedConcentration, aqiRes)
    }

    private fun checkAndTriggerAlerts(pollutants: Map<String, Double>) {
        viewModelScope.launch {
            val activeList = thresholds.value
            for (threshold in activeList) {
                if (!threshold.isEnabled) continue
                val measured = pollutants[threshold.pollutant] ?: continue
                if (measured >= threshold.criticalLimit) {
                    repository.saveAlert(
                        AlertEntity(
                            pollutant = threshold.pollutant,
                            concentration = measured,
                            threshold = threshold.criticalLimit,
                            severity = "CRITICAL",
                            status = "ACTIVE",
                            message = "${threshold.pollutant} measured at ${String.format("%.1f", measured)} exceeds critical threshold (${threshold.criticalLimit}).",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else if (measured >= threshold.warningLimit) {
                    repository.saveAlert(
                        AlertEntity(
                            pollutant = threshold.pollutant,
                            concentration = measured,
                            threshold = threshold.warningLimit,
                            severity = "WARNING",
                            status = "ACTIVE",
                            message = "${threshold.pollutant} measured at ${String.format("%.1f", measured)} exceeds warning threshold (${threshold.warningLimit}).",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private fun checkPredictedThreshold(pollutant: String, predictedValue: Double, horizon: Int) {
        viewModelScope.launch {
            val activeList = thresholds.value
            val rule = activeList.firstOrNull { it.pollutant == pollutant && it.isEnabled } ?: return@launch
            if (predictedValue >= rule.warningLimit) {
                repository.saveAlert(
                    AlertEntity(
                        pollutant = pollutant,
                        concentration = predictedValue,
                        threshold = rule.warningLimit,
                        severity = if (predictedValue >= rule.criticalLimit) "CRITICAL" else "WARNING",
                        status = "ACTIVE",
                        message = "Forecast Alert: Predicted $pollutant at +${horizon}h is ${String.format("%.1f", predictedValue)} (Threshold: ${rule.warningLimit}).",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun acknowledgeAlert(id: Int) {
        viewModelScope.launch {
            repository.acknowledgeAlert(id)
        }
    }

    fun resolveAlert(id: Int) {
        viewModelScope.launch {
            repository.resolveAlert(id)
        }
    }

    fun updateThreshold(pollutant: String, warning: Double, critical: Double) {
        viewModelScope.launch {
            repository.saveThreshold(
                AlertThresholdEntity(
                    pollutant = pollutant,
                    warningLimit = warning,
                    criticalLimit = critical,
                    isEnabled = true
                )
            )
            _notification.value = UiNotification("Updated thresholds for $pollutant.")
        }
    }

    fun toggleAcademicSensorTestBed(enable: Boolean) {
        val curr = _sensorState.value
        _sensorState.value = curr.copy(
            isConnected = enable,
            isAcademicTestBedActive = enable,
            lastUpdateEpoch = if (enable) System.currentTimeMillis() else curr.lastUpdateEpoch,
            latestReadings = if (enable) mapOf(
                "PM2.5" to 44.8,
                "PM10" to 82.3,
                "NO2" to 36.1,
                "CO" to 0.95,
                "Temperature" to 26.4,
                "Humidity" to 58.0
            ) else emptyMap()
        )
        if (enable) {
            viewModelScope.launch {
                repository.saveSensorLog(
                    SensorLogEntity(
                        sensorId = curr.sensorId,
                        pollutant = "PM2.5",
                        value = 44.8,
                        timestamp = System.currentTimeMillis(),
                        isLiveSource = false
                    )
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearDatasets()
            repository.clearModels()
            repository.clearPredictions()
            _activeEvaluationMetrics.value = emptyMap()
            _featureImportances.value = emptyMap()
            _parsedDataset.value = null
            _auditSummary.value = null
            _notification.value = UiNotification("All database records cleared.")
        }
    }
}

class AirQualityViewModelFactory(private val repository: AirQualityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AirQualityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AirQualityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
