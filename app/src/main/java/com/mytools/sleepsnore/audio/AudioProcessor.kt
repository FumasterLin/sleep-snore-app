package com.mytools.sleepsnore.audio

import kotlin.math.*

class AudioProcessor(private val fftSize: Int = AudioConfig.FFT_SIZE) {

    private val hannWindow = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * Math.PI * i / (fftSize - 1)))).toFloat()
    }

    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)

    data class FrameAnalysis(
        val rms: Float,
        val db: Float,
        val snoreRatio: Float,     // 100~1000Hz 能量占总能量比例
        val isSnoringCandidate: Boolean,
        val tongueBaseRatio: Float = 0f, // 100~320Hz 舌后坠低频
        val palatalRatio: Float = 0f,    // 350~850Hz 软腭/悬雍垂
        val gaspRatio: Float = 0f        // >950Hz 气道阻塞爆破/喘鸣
    )

    /**
     * 计算单帧 PCM (ShortArray, size=512) 的时域能量与频域特征
     */
    fun processFrame(buffer: ShortArray, noiseFloorDb: Float): FrameAnalysis {
        val n = min(buffer.size, fftSize)
        var sumSquares = 0.0

        for (i in 0 until n) {
            val sample = buffer[i].toFloat()
            sumSquares += (sample * sample).toDouble()
            // 加汉宁窗
            real[i] = sample * hannWindow[i]
            imag[i] = 0f
        }
        for (i in n until fftSize) {
            real[i] = 0f
            imag[i] = 0f
        }

        // 1. 计算均方根 RMS
        val rms = sqrt(sumSquares / n).toFloat()

        // 2. 转换为分贝 (环境声学校准)
        // 满量程 32767 对应 ~90dB SPL，静音室约 25~30dB
        val db = if (rms > 1.0f) {
            (20.0 * log10(rms.toDouble() / 32768.0) + 90.0).toFloat().coerceIn(20f, 100f)
        } else {
            20f
        }

        // 3. 运行 FFT 频域变换
        fft(real, imag)

        // 4. 提取频段能量
        val hzPerBin = AudioConfig.SAMPLE_RATE.toDouble() / fftSize
        val lowBin = max(1, (AudioConfig.SNORE_FREQ_LOW / hzPerBin).toInt())
        val highBin = min(fftSize / 2 - 1, (AudioConfig.SNORE_FREQ_HIGH / hzPerBin).toInt())

        val tongueBinLow = max(1, (100.0 / hzPerBin).toInt())
        val tongueBinHigh = min(fftSize / 2 - 1, (320.0 / hzPerBin).toInt())

        val palatalBinLow = max(1, (350.0 / hzPerBin).toInt())
        val palatalBinHigh = min(fftSize / 2 - 1, (850.0 / hzPerBin).toInt())

        val gaspBinLow = max(1, (950.0 / hzPerBin).toInt())
        val gaspBinHigh = min(fftSize / 2 - 1, (2200.0 / hzPerBin).toInt())

        var snoreBandEnergy = 0.0
        var tongueBandEnergy = 0.0
        var palatalBandEnergy = 0.0
        var gaspBandEnergy = 0.0
        var totalEnergy = 0.0

        for (bin in 1 until fftSize / 2) {
            val magSquared = (real[bin] * real[bin] + imag[bin] * imag[bin]).toDouble()
            totalEnergy += magSquared
            if (bin in lowBin..highBin) {
                snoreBandEnergy += magSquared
            }
            if (bin in tongueBinLow..tongueBinHigh) {
                tongueBandEnergy += magSquared
            }
            if (bin in palatalBinLow..palatalBinHigh) {
                palatalBandEnergy += magSquared
            }
            if (bin in gaspBinLow..gaspBinHigh) {
                gaspBandEnergy += magSquared
            }
        }

        val snoreRatio = if (totalEnergy > 1e-6) {
            (snoreBandEnergy / totalEnergy).toFloat()
        } else {
            0f
        }

        val tongueRatio = if (snoreBandEnergy > 1e-6) {
            (tongueBandEnergy / snoreBandEnergy).toFloat().coerceIn(0f, 1f)
        } else 0f

        val palatalRatio = if (snoreBandEnergy > 1e-6) {
            (palatalBandEnergy / snoreBandEnergy).toFloat().coerceIn(0f, 1f)
        } else 0f

        val gaspRatio = if (totalEnergy > 1e-6) {
            (gaspBandEnergy / totalEnergy).toFloat().coerceIn(0f, 1f)
        } else 0f

        // 5. 判定打鼾候选条件：
        // 比环境底噪高出至少 6dB 且集中在低频共振频段 (> 40% 能量在 100~1000Hz)
        val isSnoringCandidate = (db >= noiseFloorDb + 6.0f) && (db >= AudioConfig.DB_MILD_THRESHOLD) && (snoreRatio >= 0.40f)

        return FrameAnalysis(
            rms = rms,
            db = db,
            snoreRatio = snoreRatio,
            isSnoringCandidate = isSnoringCandidate,
            tongueBaseRatio = tongueRatio,
            palatalRatio = palatalRatio,
            gaspRatio = gaspRatio
        )
    }

    /**
     * 原地基-2 Cooley-Tukey 快速傅里叶变换 (FFT)
     */
    private fun fft(r: FloatArray, im: FloatArray) {
        val n = r.size
        // 位反转置换 (Bit reversal)
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = r[i]; r[i] = r[j]; r[j] = tempR
                val tempI = im[i]; im[i] = im[j]; im[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // 蝶形运算
        var len = 2
        while (len <= n) {
            val half = len shr 1
            val angle = -2.0 * Math.PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until half) {
                    val pos = i + k + half
                    val uR = r[i + k]
                    val uI = im[i + k]
                    val vR = r[pos] * wR - im[pos] * wI
                    val vI = r[pos] * wI + im[pos] * wR

                    r[i + k] = uR + vR
                    im[i + k] = uI + vI
                    r[pos] = uR - vR
                    im[pos] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
