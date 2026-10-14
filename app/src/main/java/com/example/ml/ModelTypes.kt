package com.example.ml

enum class ModelAlgorithm(val displayName: String, val category: String) {
    RANDOM_FOREST("Random Forest Regressor", "Ensemble Tree Learning"),
    DECISION_TREE("Decision Tree Regressor", "CART Regression"),
    KNN("K-Nearest Neighbors", "Instance-Based"),
    XGBOOST("XGBoost Regressor", "Gradient Tree Boosting"),
    CNN_1D("1D CNN Time-Series", "Deep Learning Convolutional Model"),
    LSTM_TIME_SERIES("LSTM Recurrent Neural Network", "Deep Learning Recurrent Model")
}

data class DatasetSplit(
    val trainX: Array<DoubleArray>,
    val trainY: DoubleArray,
    val testX: Array<DoubleArray>,
    val testY: DoubleArray,
    val featureNames: List<String>,
    val targetName: String,
    val trainTimestamps: List<String>,
    val testTimestamps: List<String>,
    val scalerMeans: DoubleArray,
    val scalerStds: DoubleArray
)

data class EvaluationMetrics(
    val mae: Double,
    val mse: Double,
    val rmse: Double,
    val r2: Double,
    val actualValues: List<Double>,
    val predictedValues: List<Double>,
    val residualValues: List<Double>,
    val testTimestamps: List<String>
)

data class FeatureImportance(
    val featureName: String,
    val score: Double, // Normalized 0..1
    val percentage: Double
)

interface AirQualityRegressor {
    val algorithm: ModelAlgorithm
    fun fit(X: Array<DoubleArray>, y: DoubleArray)
    fun predict(X: Array<DoubleArray>): DoubleArray
    fun predictSingle(x: DoubleArray): Double
    fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance>
}
