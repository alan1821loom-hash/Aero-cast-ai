package com.example.ml

import kotlin.math.max

class RandomForestRegressor(
    private val nEstimators: Int = 15,
    private val maxDepth: Int = 9,
    private val minSamplesSplit: Int = 4,
    private val maxFeaturesRatio: Double = 0.75,
    private val randomSeed: Long = 42L
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.RANDOM_FOREST

    private val trees = ArrayList<DecisionTreeRegressor>()
    private var cachedImportances: List<FeatureImportance> = emptyList()

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        trees.clear()
        if (X.isEmpty() || y.isEmpty()) return

        val n = X.size
        val rng = java.util.Random(randomSeed)

        for (i in 0 until nEstimators) {
            // Bootstrap sample
            val bootX = Array(n) { DoubleArray(X[0].size) }
            val bootY = DoubleArray(n)

            for (j in 0 until n) {
                val sampleIdx = rng.nextInt(n)
                System.arraycopy(X[sampleIdx], 0, bootX[j], 0, X[sampleIdx].size)
                bootY[j] = y[sampleIdx]
            }

            val tree = DecisionTreeRegressor(
                maxDepth = maxDepth,
                minSamplesSplit = minSamplesSplit,
                maxFeaturesRatio = maxFeaturesRatio,
                randomSeed = randomSeed + i * 17L
            )
            tree.fit(bootX, bootY)
            trees.add(tree)
        }
    }

    override fun predict(X: Array<DoubleArray>): DoubleArray {
        val result = DoubleArray(X.size)
        if (trees.isEmpty()) return result

        for (i in X.indices) {
            result[i] = predictSingle(X[i])
        }
        return result
    }

    override fun predictSingle(x: DoubleArray): Double {
        if (trees.isEmpty()) return 0.0
        var sum = 0.0
        for (tree in trees) {
            sum += tree.predictSingle(x)
        }
        return sum / trees.size
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        if (trees.isEmpty()) return emptyList()

        val accum = DoubleArray(featureNames.size)
        for (tree in trees) {
            val list = tree.getFeatureImportances(featureNames)
            for (item in list) {
                val idx = featureNames.indexOf(item.featureName)
                if (idx >= 0) {
                    accum[idx] += item.score
                }
            }
        }

        val total = accum.sum()
        return featureNames.mapIndexed { idx, name ->
            val score = if (total > 0) accum[idx] / total else 0.0
            FeatureImportance(
                featureName = name,
                score = score,
                percentage = score * 100.0
            )
        }.sortedByDescending { it.score }
    }
}
