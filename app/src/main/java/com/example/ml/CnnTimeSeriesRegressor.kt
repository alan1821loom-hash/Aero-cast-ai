package com.example.ml

import kotlin.math.max
import kotlin.math.sqrt

class CnnTimeSeriesRegressor(
    val windowSize: Int = 6,
    private val numFilters: Int = 8,
    private val kernelSize: Int = 3,
    private val epochs: Int = 35,
    private val learningRate: Double = 0.015,
    private val randomSeed: Long = 42L
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.CNN_1D

    val epochLossHistory = ArrayList<Double>()
    var insufficientDataMessage: String? = null
        private set

    private var weightsConv: Array<Array<DoubleArray>> = emptyArray() // [filter][kernel][feature]
    private var biasConv: DoubleArray = DoubleArray(0)
    private var weightsDense: DoubleArray = DoubleArray(0) // [flattened_size]
    private var biasDense: Double = 0.0

    private var inputFeatureCount: Int = 0
    private var convOutLength: Int = 0
    private var flattenedSize: Int = 0

    private var featureMeans: DoubleArray = DoubleArray(0)
    private var featureStds: DoubleArray = DoubleArray(0)
    private var targetMean: Double = 0.0
    private var targetStd: Double = 1.0

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        epochLossHistory.clear()
        insufficientDataMessage = null

        val n = X.size
        if (n < windowSize + 10) {
            insufficientDataMessage = "Insufficient sequential data: Required at least ${windowSize + 10} time steps for window length $windowSize, but only $n rows provided."
            return
        }

        inputFeatureCount = X[0].size
        convOutLength = windowSize - kernelSize + 1
        flattenedSize = numFilters * convOutLength

        // Compute normalization
        featureMeans = DoubleArray(inputFeatureCount)
        featureStds = DoubleArray(inputFeatureCount)
        for (j in 0 until inputFeatureCount) {
            var sum = 0.0
            for (i in 0 until n) sum += X[i][j]
            val m = sum / n
            featureMeans[j] = m

            var varSum = 0.0
            for (i in 0 until n) {
                val d = X[i][j] - m
                varSum += d * d
            }
            val s = sqrt(varSum / n)
            featureStds[j] = if (s > 1e-6) s else 1.0
        }

        var ySum = 0.0
        for (v in y) ySum += v
        targetMean = ySum / n
        var yVar = 0.0
        for (v in y) {
            val d = v - targetMean
            yVar += d * d
        }
        val yStd = sqrt(yVar / n)
        targetStd = if (yStd > 1e-6) yStd else 1.0

        // Create time windows
        val numWindows = n - windowSize
        val windowX = Array(numWindows) { w ->
            Array(windowSize) { t ->
                DoubleArray(inputFeatureCount) { f ->
                    (X[w + t][f] - featureMeans[f]) / featureStds[f]
                }
            }
        }
        val windowY = DoubleArray(numWindows) { w ->
            (y[w + windowSize] - targetMean) / targetStd
        }

        // Initialize parameters with Xavier / He uniform
        val rng = java.util.Random(randomSeed)
        val limitConv = sqrt(6.0 / (kernelSize * inputFeatureCount + numFilters))
        weightsConv = Array(numFilters) {
            Array(kernelSize) {
                DoubleArray(inputFeatureCount) {
                    (rng.nextDouble() * 2.0 - 1.0) * limitConv
                }
            }
        }
        biasConv = DoubleArray(numFilters) { 0.0 }

        val limitDense = sqrt(6.0 / (flattenedSize + 1))
        weightsDense = DoubleArray(flattenedSize) {
            (rng.nextDouble() * 2.0 - 1.0) * limitDense
        }
        biasDense = 0.0

        // Training loop with mini-batch SGD + Momentum
        val momentumDense = DoubleArray(flattenedSize)
        val momentumConv = Array(numFilters) { Array(kernelSize) { DoubleArray(inputFeatureCount) } }
        val beta = 0.85

        for (epoch in 0 until epochs) {
            var totalLoss = 0.0
            val indices = (0 until numWindows).shuffled(rng)

            for (idx in indices) {
                val inputSeq = windowX[idx]
                val target = windowY[idx]

                // Forward pass: 1D Convolution
                // convOut: [numFilters][convOutLength]
                val convOut = Array(numFilters) { DoubleArray(convOutLength) }
                for (f in 0 until numFilters) {
                    for (t in 0 until convOutLength) {
                        var sum = biasConv[f]
                        for (k in 0 until kernelSize) {
                            for (c in 0 until inputFeatureCount) {
                                sum += inputSeq[t + k][c] * weightsConv[f][k][c]
                            }
                        }
                        // ReLU activation
                        convOut[f][t] = max(0.0, sum)
                    }
                }

                // Flatten
                val flat = DoubleArray(flattenedSize)
                var p = 0
                for (f in 0 until numFilters) {
                    for (t in 0 until convOutLength) {
                        flat[p++] = convOut[f][t]
                    }
                }

                // Dense forward
                var predNorm = biasDense
                for (i in 0 until flattenedSize) {
                    predNorm += flat[i] * weightsDense[i]
                }

                // MSE Loss
                val error = predNorm - target
                totalLoss += error * error

                // Backward pass:
                val dPred = 2.0 * error

                // Dense grads
                val dFlat = DoubleArray(flattenedSize)
                for (i in 0 until flattenedSize) {
                    dFlat[i] = dPred * weightsDense[i]
                    val gradW = dPred * flat[i]
                    momentumDense[i] = beta * momentumDense[i] + (1.0 - beta) * gradW
                    weightsDense[i] -= learningRate * momentumDense[i]
                }
                biasDense -= learningRate * dPred

                // Conv grads (ReLU derivative & backprop)
                p = 0
                for (f in 0 until numFilters) {
                    var dBias = 0.0
                    for (t in 0 until convOutLength) {
                        val dAct = dFlat[p++]
                        val dPreRelu = if (convOut[f][t] > 0) dAct else 0.0
                        dBias += dPreRelu

                        for (k in 0 until kernelSize) {
                            for (c in 0 until inputFeatureCount) {
                                val gradKernel = dPreRelu * inputSeq[t + k][c]
                                momentumConv[f][k][c] = beta * momentumConv[f][k][c] + (1.0 - beta) * gradKernel
                                weightsConv[f][k][c] -= learningRate * momentumConv[f][k][c]
                            }
                        }
                    }
                    biasConv[f] -= learningRate * dBias
                }
            }

            val epochMse = totalLoss / numWindows
            epochLossHistory.add(epochMse)
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
        if (weightsDense.isEmpty() || x.isEmpty()) return targetMean

        // For single-step inference, replicate current features into window
        val p = x.size
        val normRow = DoubleArray(p) { j ->
            (x[j] - featureMeans.getOrElse(j) { 0.0 }) / featureStds.getOrElse(j) { 1.0 }
        }

        val inputSeq = Array(windowSize) { normRow }

        val convOut = Array(numFilters) { DoubleArray(convOutLength) }
        for (f in 0 until numFilters) {
            for (t in 0 until convOutLength) {
                var sum = biasConv[f]
                for (k in 0 until kernelSize) {
                    for (c in 0 until p) {
                        sum += inputSeq[t + k][c] * weightsConv[f][k][c]
                    }
                }
                convOut[f][t] = max(0.0, sum)
            }
        }

        var denseOut = biasDense
        var idx = 0
        for (f in 0 until numFilters) {
            for (t in 0 until convOutLength) {
                denseOut += convOut[f][t] * weightsDense[idx++]
            }
        }

        return (denseOut * targetStd) + targetMean
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        if (weightsConv.isEmpty()) return emptyList()
        val p = featureNames.size
        val accum = DoubleArray(p)

        for (f in 0 until numFilters) {
            for (k in 0 until kernelSize) {
                for (c in 0 until p) {
                    val w = weightsConv[f][k][c]
                    accum[c] += w * w
                }
            }
        }

        val total = accum.sum()
        return featureNames.mapIndexed { idx, name ->
            val s = if (total > 0) accum[idx] / total else 0.0
            FeatureImportance(
                featureName = name,
                score = s,
                percentage = s * 100.0
            )
        }.sortedByDescending { it.score }
    }
}
