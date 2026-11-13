package com.sentup.android.ml

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sentup.android.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * On-device Sensor & Environment Adaptive Engine covering:
 * - #21 Walking/Running Cadence Sync (Accelerometer/Step Detector SPM -> Track Pace Match)
 * - #22 Sleep Motion Stillness Detector (triggers gentle logarithmic fade when stationary)
 * - #23 Environment Noise Adaptive EQ Profiles (Quiet / Commute / Car / Gym)
 */
enum class EnvironmentAudioMode {
    NORMAL,
    QUIET_NIGHT,
    COMMUTE_TRANSIT,
    CAR_DRIVE,
    WORKOUT_RUN
}

class SensorAndEnvironmentEngine(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _estimatedSpm = MutableStateFlow(0)
    val estimatedSpm: StateFlow<Int> = _estimatedSpm.asStateFlow()

    private val _isStationaryForSleep = MutableStateFlow(false)
    val isStationaryForSleep: StateFlow<Boolean> = _isStationaryForSleep.asStateFlow()

    private val _environmentMode = MutableStateFlow(EnvironmentAudioMode.NORMAL)
    val environmentMode: StateFlow<EnvironmentAudioMode> = _environmentMode.asStateFlow()

    private var lastStepMs = 0L
    private var lastMotionMs = System.currentTimeMillis()
    private var smoothedMagnitude = 9.8f
    private var monitoring = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val mag = sqrt(x * x + y * y + z * z)
            val delta = abs(mag - smoothedMagnitude)
            smoothedMagnitude = 0.85f * smoothedMagnitude + 0.15f * mag
            val now = System.currentTimeMillis()

            if (delta > 0.6f) {
                lastMotionMs = now
                _isStationaryForSleep.value = false
            } else if (now - lastMotionMs > 10 * 60_000L) {
                _isStationaryForSleep.value = true
            }

            // Simple peak step cadence estimator (SPM)
            if (delta > 2.2f && now - lastStepMs > 280L) {
                val intervalMs = (now - lastStepMs).coerceIn(280L, 1500L)
                lastStepMs = now
                val instantSpm = (60_000L / intervalMs).toInt().coerceIn(50, 200)
                val prev = _estimatedSpm.value
                _estimatedSpm.value = if (prev == 0) instantSpm else ((prev * 0.7f + instantSpm * 0.3f).toInt())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun startMonitoring() {
        if (monitoring || accelSensor == null) return
        monitoring = true
        sensorManager?.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopMonitoring() {
        if (!monitoring) return
        monitoring = false
        sensorManager?.unregisterListener(listener)
    }

    fun setEnvironmentMode(mode: EnvironmentAudioMode) {
        _environmentMode.value = mode
    }

    /**
     * #23: Returns adaptive 5-band EQ gains (dB) tuned for the selected listening environment.
     */
    fun adaptiveEqGainsForMode(mode: EnvironmentAudioMode = _environmentMode.value): List<Float> = when (mode) {
        EnvironmentAudioMode.NORMAL -> listOf(0f, 0f, 0f, 0f, 0f)
        EnvironmentAudioMode.QUIET_NIGHT -> listOf(-1.5f, 0f, 1.5f, 0.5f, -1.0f)
        EnvironmentAudioMode.COMMUTE_TRANSIT -> listOf(2.5f, 1.0f, 2.0f, 1.0f, 0.5f)
        EnvironmentAudioMode.CAR_DRIVE -> listOf(3.0f, 1.5f, 2.5f, 1.5f, 1.0f)
        EnvironmentAudioMode.WORKOUT_RUN -> listOf(3.5f, 2.0f, 0.5f, 1.5f, 2.0f)
    }

    /**
     * #21: Ranks songs whose normalized pace/energy best matches the current step cadence (SPM).
     */
    fun matchSongsToCadence(songs: List<Song>, spm: Int = _estimatedSpm.value): List<Song> {
        val targetPace = ((spm.takeIf { it > 0 } ?: 120) - 60).coerceIn(0, 120) / 120f
        return songs.sortedBy { song ->
            val dna = AudioFeatureAndDspEngine.extractSongDna(song)
            abs(dna.tempoIndex - targetPace)
        }
    }
}
