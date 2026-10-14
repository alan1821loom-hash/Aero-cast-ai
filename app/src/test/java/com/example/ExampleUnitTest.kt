package com.example

import com.example.ml.AQICalculator
import com.example.ml.AcademicBenchmarkDataset
import com.example.ml.CnnTimeSeriesRegressor
import com.example.ml.DecisionTreeRegressor
import com.example.ml.GradientBoostingRegressor
import com.example.ml.KNNRegressor
import com.example.ml.MetricsCalculator
import com.example.ml.Preprocessor
import com.example.ml.RandomForestRegressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAqiBreakpointCalculation() {
        // CPCB PM2.5 test: 25 ug/m3 is Good (0-50)
        val resGood = AQICalculator.calculateAqi(mapOf("PM2.5" to 25.0))
        assertEquals("Good", resGood.category)
        assertTrue(resGood.aqiValue in 0..50)

        // CPCB PM2.5 test: 75 ug/m3 is Moderate (101-200)
        val resMod = AQICalculator.calculateAqi(mapOf("PM2.5" to 75.0))
        assertEquals("Moderate", resMod.category)
        assertTrue(resMod.aqiValue in 101..200)

        // CPCB PM2.5 test: 150 ug/m3 is Very Poor (301-400)
        val resVeryPoor = AQICalculator.calculateAqi(mapOf("PM2.5" to 150.0))
        assertEquals("Very Poor", resVeryPoor.category)
        assertTrue(resVeryPoor.aqiValue in 301..400)
    }

    @Test
    fun testMetricsCalculation() {
        val actual = doubleArrayOf(10.0, 20.0, 30.0, 40.0)
        val predicted = doubleArrayOf(11.0, 19.0, 32.0, 39.0)

        val metrics = MetricsCalculator.evaluate(actual, predicted)
        // MAE = (1 + 1 + 2 + 1) / 4 = 1.25
        assertEquals(1.25, metrics.mae, 0.001)
        // MSE = (1 + 1 + 4 + 1) / 4 = 1.75
        assertEquals(1.75, metrics.mse, 0.001)
        assertTrue(metrics.r2 > 0.95)
    }

    @Test
    fun testCsvParserAndPreprocessing() {
        val sampleCsv = AcademicBenchmarkDataset.generateCsv()
        val parsed = Preprocessor.parseCsv(sampleCsv)

        assertTrue(parsed.headers.contains("Timestamp"))
        assertTrue(parsed.headers.any { it.contains("PM2.5") })
        assertTrue(parsed.rows.size >= 100)

        val target = parsed.detectedTargetCandidate
        val features = parsed.headers.filter { it != parsed.detectedTimestampColumn && it != target }

        val (split, audit) = Preprocessor.preprocessAndSplit(
            parsed = parsed,
            targetColumn = target,
            selectedFeatures = features,
            trainRatio = 0.8
        )

        assertEquals(features.size, split.featureNames.size)
        assertTrue(split.trainX.isNotEmpty())
        assertTrue(split.testX.isNotEmpty())
        assertEquals(audit.trainCount, split.trainX.size)
        assertEquals(audit.testCount, split.testX.size)
    }

    @Test
    fun testMachineLearningModelsFittingAndInference() {
        // Simple synthetic linear data
        val n = 40
        val X = Array(n) { i -> doubleArrayOf(i.toDouble(), (i * 2).toDouble()) }
        val y = DoubleArray(n) { i -> (i * 1.5 + 5.0) }

        // Test Decision Tree
        val dt = DecisionTreeRegressor(maxDepth = 5)
        dt.fit(X, y)
        val dtPred = dt.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(dtPred > 0)

        // Test Random Forest
        val rf = RandomForestRegressor(nEstimators = 5, maxDepth = 5)
        rf.fit(X, y)
        val rfPred = rf.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(rfPred > 0)

        // Test KNN
        val knn = KNNRegressor(k = 3)
        knn.fit(X, y)
        val knnPred = knn.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(knnPred > 0)

        // Test XGBoost
        val xgb = GradientBoostingRegressor(nEstimators = 5)
        xgb.fit(X, y)
        val xgbPred = xgb.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(xgbPred > 0)

        // Test 1D CNN Time-Series
        val cnn = CnnTimeSeriesRegressor(windowSize = 4, epochs = 10)
        cnn.fit(X, y)
        val cnnPred = cnn.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(cnnPred > 0)

        // Test LSTM Time-Series Deep Learning Model
        val lstm = com.example.ml.LstmTimeSeriesRegressor(windowSize = 4, hiddenUnits = 4, epochs = 10)
        lstm.fit(X, y)
        val lstmPred = lstm.predictSingle(doubleArrayOf(10.0, 20.0))
        assertTrue(lstmPred > 0)
    }
}
