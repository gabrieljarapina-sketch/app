package com.fullhipertrofia.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.util.Locale

/**
 * Puente nativo que la app web usa como window.AndroidFH:
 *  setReminders(Boolean), notifyNow(String), startSteps(), steps(),
 *  startVoice(), stopVoice()  (y responde con window.fhVoice / window.fhVoiceOff).
 */
class Bridge(private val act: MainActivity) : SensorEventListener {

    private val handler = Handler(Looper.getMainLooper())
    private val prefs = act.getSharedPreferences("fh", Context.MODE_PRIVATE)

    // ---------- avisos ----------
    @JavascriptInterface
    fun setReminders(on: Boolean) {
        Reminders.set(act, on)
        if (on && Build.VERSION.SDK_INT >= 33) {
            act.runOnUiThread { act.ask(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { } }
        }
    }

    @JavascriptInterface
    fun notifyNow(msg: String) {
        Notifier.show(act, msg)
    }

    // ---------- pasos ----------
    @Volatile private var stepVal = -1L
    private var stepsRegistered = false

    init {
        if (prefs.getBoolean("steps", false)) {
            act.runOnUiThread { registerSteps() }
        }
    }

    private fun registerSteps() {
        if (stepsRegistered) return
        if (Build.VERSION.SDK_INT >= 29 &&
            act.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val sm = act.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val s = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        stepsRegistered = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)
    }

    @JavascriptInterface
    fun startSteps() {
        prefs.edit().putBoolean("steps", true).apply()
        act.runOnUiThread {
            val perms = if (Build.VERSION.SDK_INT >= 29) arrayOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyArray()
            act.ask(perms) { ok -> if (ok) registerSteps() }
        }
    }

    /** Contador acumulado del sensor (o -1 si todavía no hay dato). */
    @JavascriptInterface
    fun steps(): Long = stepVal

    override fun onSensorChanged(e: SensorEvent) {
        if (e.values.isNotEmpty()) stepVal = e.values[0].toLong()
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}

    // ---------- voz ----------
    private var rec: SpeechRecognizer? = null
    private var voiceOn = false
    private var errs = 0

    private fun js(code: String) {
        act.runOnUiThread { act.web.evaluateJavascript(code, null) }
    }

    @JavascriptInterface
    fun startVoice() {
        act.runOnUiThread {
            if (!SpeechRecognizer.isRecognitionAvailable(act)) {
                js("window.fhVoiceOff&&window.fhVoiceOff('no')")
                return@runOnUiThread
            }
            act.ask(arrayOf(Manifest.permission.RECORD_AUDIO)) { ok ->
                if (!ok) {
                    js("window.fhVoiceOff&&window.fhVoiceOff('perm')")
                } else {
                    voiceOn = true
                    errs = 0
                    listen()
                }
            }
        }
    }

    @JavascriptInterface
    fun stopVoice() {
        act.runOnUiThread { stopVoiceNow() }
    }

    private fun stopVoiceNow() {
        voiceOn = false
        handler.removeCallbacksAndMessages(null)
        rec?.destroy()
        rec = null
    }

    /** Si la app pasa a segundo plano se corta el micrófono y la web se entera. */
    fun pauseVoice() {
        if (voiceOn) {
            stopVoiceNow()
            js("window.fhVoiceOff&&window.fhVoiceOff('')")
        }
    }

    fun shutdown() {
        stopVoiceNow()
        if (stepsRegistered) {
            (act.getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
            stepsRegistered = false
        }
    }

    private fun again(ms: Long) {
        handler.postDelayed({ listen() }, ms)
    }

    private fun off(reason: String) {
        stopVoiceNow()
        js("window.fhVoiceOff&&window.fhVoiceOff('$reason')")
    }

    private fun listen() {
        if (!voiceOn) return
        rec?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(act)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}

            override fun onResults(b: Bundle?) {
                errs = 0
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!t.isNullOrBlank()) {
                    js("window.fhVoice&&window.fhVoice(" + JSONObject.quote(t) + ")")
                }
                again(250)
            }

            override fun onError(e: Int) {
                when (e) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> off("perm")
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> again(250)
                    else -> {
                        errs++
                        if (errs > 5) off("no") else again(1000)
                    }
                }
            }
        })
        val lang = if (Locale.getDefault().language == "es") Locale.getDefault().toLanguageTag() else "es-ES"
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        r.startListening(i)
    }
}
