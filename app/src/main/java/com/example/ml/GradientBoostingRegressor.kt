package com.example.ml

class GradientBoostingRegressor(
    private val nEstimators: Int = 20,
    private val learningRate: Double = 0.1,
    private val maxDepth: Int = 4,
    private val minSamplesSplit: Int = 4,
    private val randomSeed: Long = 42L
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.XGBOOST

    private var basePrediction: Double = 0.0
    private val estimators = ArrayList<DecisionTreeRegressor>()

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        estimators.clear()
        if (X.isEmpty() || y.isEmpty()) return

        val n = X.size
        var sum = 0.0
        for (v in y) sum += v
        basePrediction = sum / n

        // Current predictions
        val currentPreds = DoubleArray(n) { basePrediction }
        val residuals = DoubleArray(n)

        for (m in 0 until nEstimators) {
            // Compute residuals
            for (i in 0 until n) {
                residuals[i] = y[i] - currentPreds[i]
            }

            // Fit shallow regression tree to pseudo-residuals
            val tree = DecisionTreeRegressor(
                maxDepth = maxDepth,
                minSamplesSplit = minSamplesSplit,
                maxFeaturesRatio = 0.8,
                randomSeed = randomSeed + m * 31L
            )
            tree.fit(X, residuals)
            estimators.add(tree)

            // Update predictions with shrinkage
            val treePreds = tree.predict(X)
            for (i in 0 until n) {
                currentPreds[i] += learningRate * treePreds[i]
            }
        }
    }

    override fun predict(X: Array<DoubleArray>): DoubleArray {
        val result = DoubleArray(X.size)
        for (i in X.indices) {
            result[i] = predictSingle(X[i])
        }
        return result
    }

    override fun predictSingle(x: DoubleArray): Double {
        var pred = basePrediction
        for (tree in estimators) {
            pred += learningRate * tree.predictSingle(x)
        }
        return pred
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        if (estimators.isEmpty()) return emptyList()

        val accum = DoubleArray(featureNames.size)
        for (tree in estimators) {
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
