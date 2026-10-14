package com.example.ml

import kotlin.math.abs
import kotlin.math.sqrt

object MetricsCalculator {

    fun evaluate(
        actual: DoubleArray,
        predicted: DoubleArray,
        timestamps: List<String> = emptyList()
    ): EvaluationMetrics {
        val n = actual.size
        require(n > 0 && n == predicted.size) { "Arrays must be non-empty and of equal length." }

        var sumAbs = 0.0
        var sumSq = 0.0
        var sumActual = 0.0

        for (i in 0 until n) {
            val diff = actual[i] - predicted[i]
            sumAbs += abs(diff)
            sumSq += diff * diff
            sumActual += actual[i]
        }

        val mae = sumAbs / n
        val mse = sumSq / n
        val rmse = sqrt(mse)

        val meanActual = sumActual / n
        var totalVariance = 0.0
        for (i in 0 until n) {
            val d = actual[i] - meanActual
            totalVariance += d * d
        }

        // R² = 1 - (SS_res / SS_tot)
        val r2 = if (totalVariance > 1e-7) {
            1.0 - (sumSq / totalVariance)
        } else {
            0.0
        }

        val actualList = actual.toList()
        val predictedList = predicted.toList()
        val residuals = actualList.mapIndexed { idx, act -> act - predictedList[idx] }

        return EvaluationMetrics(
            mae = mae,
            mse = mse,
            rmse = rmse,
            r2 = r2,
            actualValues = actualList,
            predictedValues = predictedList,
            residualValues = residuals,
            testTimestamps = timestamps
        )
    }
}
