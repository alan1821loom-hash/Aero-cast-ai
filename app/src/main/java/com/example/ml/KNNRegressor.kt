package com.example.ml

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.sqrt

class KNNRegressor(
    private val k: Int = 5,
    private val weighted: Boolean = true
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.KNN

    private var trainX: Array<DoubleArray> = emptyArray()
    private var trainY: DoubleArray = DoubleArray(0)
    private var featureMeans: DoubleArray = DoubleArray(0)
    private var featureStds: DoubleArray = DoubleArray(0)

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        if (X.isEmpty() || y.isEmpty()) return
        val n = X.size
        val p = X[0].size

        featureMeans = DoubleArray(p)
        featureStds = DoubleArray(p)

        for (j in 0 until p) {
            var sum = 0.0
            for (i in 0 until n) sum += X[i][j]
            val mean = sum / n
            featureMeans[j] = mean

            var varSum = 0.0
            for (i in 0 until n) {
                val d = X[i][j] - mean
                varSum += d * d
            }
            val std = sqrt(varSum / n)
            featureStds[j] = if (std > 1e-6) std else 1.0
        }

        // Standardize stored trainX
        trainX = Array(n) { i ->
            DoubleArray(p) { j ->
                (X[i][j] - featureMeans[j]) / featureStds[j]
            }
        }
        trainY = y.clone()
    }

    private data class Neighbor(val distance: Double, val target: Double) : Comparable<Neighbor> {
        override fun compareTo(other: Neighbor): Int = other.distance.compareTo(this.distance) // max heap
    }

    override fun predict(X: Array<DoubleArray>): DoubleArray {
        val result = DoubleArray(X.size)
        for (i in X.indices) {
            result[i] = predictSingle(X[i])
        }
        return result
    }

    override fun predictSingle(x: DoubleArray): Double {
        if (trainX.isEmpty()) return 0.0
        val p = x.size
        val normalizedX = DoubleArray(p) { j ->
            (x[j] - featureMeans.getOrElse(j) { 0.0 }) / featureStds.getOrElse(j) { 1.0 }
        }

        val queue = PriorityQueue<Neighbor>()
        val effectiveK = kotlin.math.min(k, trainX.size)

        for (i in trainX.indices) {
            var distSq = 0.0
            val row = trainX[i]
            for (j in 0 until p) {
                val diff = row[j] - normalizedX[j]
                distSq += diff * diff
            }
            val dist = sqrt(distSq)

            if (queue.size < effectiveK) {
                queue.add(Neighbor(dist, trainY[i]))
            } else if (dist < queue.peek()!!.distance) {
                queue.poll()
                queue.add(Neighbor(dist, trainY[i]))
            }
        }

        if (queue.isEmpty()) return 0.0

        if (!weighted) {
            var sum = 0.0
            for (n in queue) sum += n.target
            return sum / queue.size
        } else {
            var weightedSum = 0.0
            var totalWeight = 0.0
            for (n in queue) {
                val weight = 1.0 / (n.distance + 1e-4)
                weightedSum += n.target * weight
                totalWeight += weight
            }
            return if (totalWeight > 0) weightedSum / totalWeight else 0.0
        }
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        // Feature correlation with target
        if (trainX.isEmpty()) return emptyList()
        val p = featureNames.size
        val scores = DoubleArray(p)

        var yMean = 0.0
        for (y in trainY) yMean += y
        yMean /= trainY.size

        for (j in 0 until p) {
            var cov = 0.0
            var varX = 0.0
            var varY = 0.0
            for (i in trainX.indices) {
                val dx = trainX[i][j]
                val dy = trainY[i] - yMean
                cov += dx * dy
                varX += dx * dx
                varY += dy * dy
            }
            val denom = sqrt(varX * varY)
            val corr = if (denom > 1e-7) abs(cov / denom) else 0.0
            scores[j] = corr
        }

        val total = scores.sum()
        return featureNames.mapIndexed { idx, name ->
            val s = if (total > 0) scores[idx] / total else 0.0
            FeatureImportance(
                featureName = name,
                score = s,
                percentage = s * 100.0
            )
        }.sortedByDescending { it.score }
    }
}
