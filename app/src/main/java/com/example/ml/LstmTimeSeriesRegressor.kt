package com.example.ml

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tanh

class LstmTimeSeriesRegressor(
    val windowSize: Int = 6,
    val hiddenUnits: Int = 8,
    private val epochs: Int = 30,
    private val learningRate: Double = 0.015,
    private val randomSeed: Long = 42L
) : AirQualityRegressor {

    override val algorithm: ModelAlgorithm = ModelAlgorithm.LSTM_TIME_SERIES

    val epochLossHistory = ArrayList<Double>()
    var insufficientDataMessage: String? = null
        private set

    private var inputFeatureCount: Int = 0

    // Gate weights: [hiddenUnits][inputFeatureCount]
    private var Wf: Array<DoubleArray> = emptyArray()
    private var Wi: Array<DoubleArray> = emptyArray()
    private var Wc: Array<DoubleArray> = emptyArray()
    private var Wo: Array<DoubleArray> = emptyArray()

    // Recurrent weights: [hiddenUnits][hiddenUnits]
    private var Uf: Array<DoubleArray> = emptyArray()
    private var Ui: Array<DoubleArray> = emptyArray()
    private var Uc: Array<DoubleArray> = emptyArray()
    private var Uo: Array<DoubleArray> = emptyArray()

    // Biases: [hiddenUnits]
    private var bf: DoubleArray = DoubleArray(0)
    private var bi: DoubleArray = DoubleArray(0)
    private var bc: DoubleArray = DoubleArray(0)
    private var bo: DoubleArray = DoubleArray(0)

    // Output dense layer
    private var Wy: DoubleArray = DoubleArray(0) // [hiddenUnits]
    private var by: Double = 0.0

    // Standardization
    private var featureMeans: DoubleArray = DoubleArray(0)
    private var featureStds: DoubleArray = DoubleArray(0)
    private var targetMean: Double = 0.0
    private var targetStd: Double = 1.0

    private fun sigmoid(x: Double): Double {
        val clamped = x.coerceIn(-15.0, 15.0)
        return 1.0 / (1.0 + exp(-clamped))
    }

    override fun fit(X: Array<DoubleArray>, y: DoubleArray) {
        epochLossHistory.clear()
        insufficientDataMessage = null

        val n = X.size
        if (n < windowSize + 10) {
            insufficientDataMessage = "Insufficient sequential observations: Required at least ${windowSize + 10} time steps for window length $windowSize, but only $n provided."
            return
        }

        inputFeatureCount = X[0].size

        // Calculate means and stds
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

        // Build chronological windows
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

        // Initialize LSTM weights using Xavier
        val rng = java.util.Random(randomSeed)
        val limitX = sqrt(6.0 / (inputFeatureCount + hiddenUnits))
        val limitH = sqrt(6.0 / (hiddenUnits + hiddenUnits))

        fun initMatrix(rows: Int, cols: Int, lim: Double) = Array(rows) {
            DoubleArray(cols) { (rng.nextDouble() * 2.0 - 1.0) * lim }
        }

        Wf = initMatrix(hiddenUnits, inputFeatureCount, limitX)
        Wi = initMatrix(hiddenUnits, inputFeatureCount, limitX)
        Wc = initMatrix(hiddenUnits, inputFeatureCount, limitX)
        Wo = initMatrix(hiddenUnits, inputFeatureCount, limitX)

        Uf = initMatrix(hiddenUnits, hiddenUnits, limitH)
        Ui = initMatrix(hiddenUnits, hiddenUnits, limitH)
        Uc = initMatrix(hiddenUnits, hiddenUnits, limitH)
        Uo = initMatrix(hiddenUnits, hiddenUnits, limitH)

        bf = DoubleArray(hiddenUnits) { 1.0 } // Forget gate bias initialized to 1 for long-term gradient flow
        bi = DoubleArray(hiddenUnits) { 0.0 }
        bc = DoubleArray(hiddenUnits) { 0.0 }
        bo = DoubleArray(hiddenUnits) { 0.0 }

        val limitY = sqrt(6.0 / (hiddenUnits + 1))
        Wy = DoubleArray(hiddenUnits) { (rng.nextDouble() * 2.0 - 1.0) * limitY }
        by = 0.0

        // Momentum buffers
        val beta = 0.85
        val momWy = DoubleArray(hiddenUnits)
        var momBy = 0.0

        // Training loop with BPTT
        for (epoch in 0 until epochs) {
            var totalLoss = 0.0
            val indices = (0 until numWindows).shuffled(rng)

            for (idx in indices) {
                val seq = windowX[idx]
                val target = windowY[idx]

                // Forward pass across sequence
                val hStates = Array(windowSize + 1) { DoubleArray(hiddenUnits) }
                val cStates = Array(windowSize + 1) { DoubleArray(hiddenUnits) }
                val fGates = Array(windowSize) { DoubleArray(hiddenUnits) }
                val iGates = Array(windowSize) { DoubleArray(hiddenUnits) }
                val cTilde = Array(windowSize) { DoubleArray(hiddenUnits) }
                val oGates = Array(windowSize) { DoubleArray(hiddenUnits) }

                for (t in 0 until windowSize) {
                    val xt = seq[t]
                    val hPrev = hStates[t]
                    val cPrev = cStates[t]

                    for (h in 0 until hiddenUnits) {
                        var sumF = bf[h]
                        var sumI = bi[h]
                        var sumC = bc[h]
                        var sumO = bo[h]

                        for (f in 0 until inputFeatureCount) {
                            val v = xt[f]
                            sumF += Wf[h][f] * v
                            sumI += Wi[h][f] * v
                            sumC += Wc[h][f] * v
                            sumO += Wo[h][f] * v
                        }
                        for (prev in 0 until hiddenUnits) {
                            val v = hPrev[prev]
                            sumF += Uf[h][prev] * v
                            sumI += Ui[h][prev] * v
                            sumC += Uc[h][prev] * v
                            sumO += Uo[h][prev] * v
                        }

                        val ft = sigmoid(sumF)
                        val itVal = sigmoid(sumI)
                        val ctTilde = tanh(sumC)
                        val ot = sigmoid(sumO)

                        fGates[t][h] = ft
                        iGates[t][h] = itVal
                        cTilde[t][h] = ctTilde
                        oGates[t][h] = ot

                        val ct = ft * cPrev[h] + itVal * ctTilde
                        cStates[t + 1][h] = ct
                        hStates[t + 1][h] = ot * tanh(ct)
                    }
                }

                // Final prediction from last hidden state
                val finalH = hStates[windowSize]
                var pred = by
                for (h in 0 until hiddenUnits) pred += Wy[h] * finalH[h]

                val err = pred - target
                totalLoss += err * err

                // Backprop gradient at output
                val dPred = 2.0 * err
                for (h in 0 until hiddenUnits) {
                    val gradWy = dPred * finalH[h]
                    momWy[h] = beta * momWy[h] + (1.0 - beta) * gradWy
                    Wy[h] -= learningRate * momWy[h]
                }
                momBy = beta * momBy + (1.0 - beta) * dPred
                by -= learningRate * momBy

                // Backpropagation through time for last time step
                val dhFinal = DoubleArray(hiddenUnits) { h -> dPred * Wy[h] }
                for (h in 0 until hiddenUnits) {
                    val ot = oGates[windowSize - 1][h]
                    val ct = cStates[windowSize][h]
                    val tanhCt = tanh(ct)

                    val dO = dhFinal[h] * tanhCt
                    val dOtPre = dO * ot * (1.0 - ot)
                    bo[h] -= learningRate * dOtPre

                    val dC = dhFinal[h] * ot * (1.0 - tanhCt * tanhCt)
                    val itVal = iGates[windowSize - 1][h]
                    val ctTilde = cTilde[windowSize - 1][h]

                    val dCtTilde = dC * itVal
                    val dCtTildePre = dCtTilde * (1.0 - ctTilde * ctTilde)
                    bc[h] -= learningRate * dCtTildePre

                    val dI = dC * ctTilde
                    val dIPre = dI * itVal * (1.0 - itVal)
                    bi[h] -= learningRate * dIPre

                    val ft = fGates[windowSize - 1][h]
                    val cPrev = cStates[windowSize - 1][h]
                    val dF = dC * cPrev
                    val dFPre = dF * ft * (1.0 - ft)
                    bf[h] -= learningRate * dFPre

                    // Update input weights for time window
                    val xt = seq[windowSize - 1]
                    for (f in 0 until inputFeatureCount) {
                        Wo[h][f] -= learningRate * dOtPre * xt[f]
                        Wc[h][f] -= learningRate * dCtTildePre * xt[f]
                        Wi[h][f] -= learningRate * dIPre * xt[f]
                        Wf[h][f] -= learningRate * dFPre * xt[f]
                    }
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
        if (Wy.isEmpty() || x.isEmpty()) return targetMean

        val p = x.size
        val normRow = DoubleArray(p) { j ->
            (x[j] - featureMeans.getOrElse(j) { 0.0 }) / featureStds.getOrElse(j) { 1.0 }
        }

        var hState = DoubleArray(hiddenUnits)
        var cState = DoubleArray(hiddenUnits)

        for (t in 0 until windowSize) {
            val nextH = DoubleArray(hiddenUnits)
            val nextC = DoubleArray(hiddenUnits)

            for (h in 0 until hiddenUnits) {
                var sumF = bf[h]
                var sumI = bi[h]
                var sumC = bc[h]
                var sumO = bo[h]

                for (f in 0 until p) {
                    val v = normRow[f]
                    sumF += Wf[h][f] * v
                    sumI += Wi[h][f] * v
                    sumC += Wc[h][f] * v
                    sumO += Wo[h][f] * v
                }
                for (prev in 0 until hiddenUnits) {
                    val v = hState[prev]
                    sumF += Uf[h][prev] * v
                    sumI += Ui[h][prev] * v
                    sumC += Uc[h][prev] * v
                    sumO += Uo[h][prev] * v
                }

                val ft = sigmoid(sumF)
                val itVal = sigmoid(sumI)
                val ctTilde = tanh(sumC)
                val ot = sigmoid(sumO)

                val ct = ft * cState[h] + itVal * ctTilde
                nextC[h] = ct
                nextH[h] = ot * tanh(ct)
            }
            hState = nextH
            cState = nextC
        }

        var pred = by
        for (h in 0 until hiddenUnits) pred += Wy[h] * hState[h]
        return (pred * targetStd) + targetMean
    }

    override fun getFeatureImportances(featureNames: List<String>): List<FeatureImportance> {
        if (Wc.isEmpty()) return emptyList()
        val p = featureNames.size
        val accum = DoubleArray(p)

        for (h in 0 until hiddenUnits) {
            for (f in 0 until p) {
                val wf = Wf[h][f]
                val wi = Wi[h][f]
                val wc = Wc[h][f]
                val wo = Wo[h][f]
                accum[f] += (wf * wf + wi * wi + wc * wc + wo * wo)
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
