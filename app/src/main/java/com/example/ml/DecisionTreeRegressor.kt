package com.example.ml

import kotlin.math.max
import kotlin.math.min

class DecisionTreeRegressor(
    private val maxDepth: Int = 8,
    private val minSamplesSplit: Int = 4,
    private val maxFeaturesRatio: Double = 1.0,
    private val randomSeed: Long = 42L
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.DECISION_TREE

    private sealed class Node {
        data class Leaf(val value: Double) : Node()
        data class Split(
            val featureIndex: Int,
            val threshold: Double,
            val left: Node,
            val right: Node,
            val impurityDecrease: Double
        ) : Node()
    }

    private var root: Node? = null
    private var featureVarianceReductions: DoubleArray = DoubleArray(0)
    private val rng = java.util.Random(randomSeed)

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        if (X.isEmpty() || y.isEmpty()) return
        val numFeatures = X[0].size
        featureVarianceReductions = DoubleArray(numFeatures)

        val indices = IntArray(X.size) { it }
        root = buildTree(X, y, indices, depth = 0)
    }

    private fun buildTree(
        X: Array<DoubleArray>,
        y: DoubleArray,
        indices: IntArray,
        depth: Int
    ): Node {
        val n = indices.size
        if (n == 0) return Node.Leaf(0.0)

        var sumY = 0.0
        for (idx in indices) sumY += y[idx]
        val meanY = sumY / n

        if (depth >= maxDepth || n <= minSamplesSplit) {
            return Node.Leaf(meanY)
        }

        var totalVar = 0.0
        for (idx in indices) {
            val diff = y[idx] - meanY
            totalVar += diff * diff
        }
        val currentMse = totalVar / n
        if (currentMse < 1e-7) {
            return Node.Leaf(meanY)
        }

        val numFeatures = X[0].size
        var bestFeature = -1
        var bestThreshold = 0.0
        var bestGain = 0.0
        var bestLeftIndices: IntArray? = null
        var bestRightIndices: IntArray? = null

        // Sample features if maxFeaturesRatio < 1.0 (for Random Forest)
        val candidateFeatures = if (maxFeaturesRatio >= 1.0) {
            (0 until numFeatures).toList()
        } else {
            val count = max(1, (numFeatures * maxFeaturesRatio).toInt())
            (0 until numFeatures).shuffled(rng).take(count)
        }

        for (feat in candidateFeatures) {
            // Get unique values to test splits (subsample thresholds if many samples for speed)
            val values = DoubleArray(n) { X[indices[it]][feat] }
            values.sort()

            val step = max(1, n / 15)
            for (i in 0 until (n - 1) step step) {
                if (values[i] == values[i + 1]) continue
                val threshold = (values[i] + values[i + 1]) / 2.0

                var leftSum = 0.0
                var leftCount = 0
                var rightSum = 0.0
                var rightCount = 0

                for (idx in indices) {
                    if (X[idx][feat] <= threshold) {
                        leftSum += y[idx]
                        leftCount++
                    } else {
                        rightSum += y[idx]
                        rightCount++
                    }
                }

                if (leftCount == 0 || rightCount == 0) continue

                val leftMean = leftSum / leftCount
                val rightMean = rightSum / rightCount

                var leftVar = 0.0
                var rightVar = 0.0
                for (idx in indices) {
                    if (X[idx][feat] <= threshold) {
                        val d = y[idx] - leftMean
                        leftVar += d * d
                    } else {
                        val d = y[idx] - rightMean
                        rightVar += d * d
                    }
                }

                val leftMse = leftVar / leftCount
                val rightMse = rightVar / rightCount
                val splitMse = (leftCount.toDouble() / n) * leftMse + (rightCount.toDouble() / n) * rightMse
                val gain = currentMse - splitMse

                if (gain > bestGain) {
                    bestGain = gain
                    bestFeature = feat
                    bestThreshold = threshold

                    val leftList = IntArrayList(leftCount)
                    val rightList = IntArrayList(rightCount)
                    for (idx in indices) {
                        if (X[idx][feat] <= threshold) leftList.add(idx) else rightList.add(idx)
                    }
                    bestLeftIndices = leftList.toArray()
                    bestRightIndices = rightList.toArray()
                }
            }
        }

        if (bestGain <= 1e-7 || bestLeftIndices == null || bestRightIndices == null) {
            return Node.Leaf(meanY)
        }

        // Accumulate feature importance
        featureVarianceReductions[bestFeature] += bestGain * n

        val leftChild = buildTree(X, y, bestLeftIndices, depth + 1)
        val rightChild = buildTree(X, y, bestRightIndices, depth + 1)

        return Node.Split(
            featureIndex = bestFeature,
            threshold = bestThreshold,
            left = leftChild,
            right = rightChild,
            impurityDecrease = bestGain
        )
    }

    override fun predict(X: Array<DoubleArray>): DoubleArray {
        val result = DoubleArray(X.size)
        for (i in X.indices) {
            result[i] = predictSingle(X[i])
        }
        return result
    }

    override fun predictSingle(x: DoubleArray): Double {
        var curr = root ?: return 0.0
        while (curr is Node.Split) {
            curr = if (x[curr.featureIndex] <= curr.threshold) {
                curr.left
            } else {
                curr.right
            }
        }
        return (curr as Node.Leaf).value
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        val total = featureVarianceReductions.sum()
        return featureNames.mapIndexed { idx, name ->
            val score = if (total > 0) featureVarianceReductions.getOrElse(idx) { 0.0 } / total else 0.0
            FeatureImportance(
                featureName = name,
                score = score,
                percentage = score * 100.0
            )
        }.sortedByDescending { it.score }
    }

    private class IntArrayList(initialCapacity: Int) {
        var data = IntArray(initialCapacity)
        var size = 0
        fun add(value: Int) {
            if (size >= data.size) data = data.copyOf(max(10, data.size * 2))
            data[size++] = value
        }
        fun toArray(): IntArray = data.copyOf(size)
    }
}
