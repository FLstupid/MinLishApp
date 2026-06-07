package com.example.minlish.logic

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.minlish.data.model.Word
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class TtsState {
    data object Initializing : TtsState()
    data object Ready : TtsState()
    data class Unavailable(val reason: UnavailableReason) : TtsState()
}

enum class UnavailableReason {
    InitFailed,
    MissingLanguageData,
}

sealed class TtsUiEvent {
    data class Unavailable(val reason: UnavailableReason) : TtsUiEvent()
    data object SpeakFailed : TtsUiEvent()
    data object VolumeMuted : TtsUiEvent()
}

private data class PendingSpeak(val text: String)

/**
 * Speaks English vocabulary only. UI may be Vietnamese; TTS never follows [Locale.getDefault].
 */
class TtsManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var englishLocale: Locale = SPEAK_LOCALE

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _uiEvents = MutableSharedFlow<TtsUiEvent>(extraBufferCapacity = 8)
    val uiEvents: SharedFlow<TtsUiEvent> = _uiEvents.asSharedFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initFinished = AtomicBoolean(false)

    @Volatile
    private var pendingSpeak: PendingSpeak? = null

    init {
        val start = {
            createEngine(preferredEngine = GOOGLE_TTS_ENGINE)
            scheduleInitTimeout()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            start()
        } else {
            mainHandler.post(start)
        }
    }

    private fun scheduleInitTimeout() {
        mainHandler.postDelayed({
            if (_state.value is TtsState.Initializing && initFinished.compareAndSet(false, true)) {
                Log.w(TAG, "TTS init timed out")
                publishState(TtsState.Unavailable(UnavailableReason.InitFailed))
            }
        }, INIT_TIMEOUT_MS)
    }

    private fun createEngine(preferredEngine: String?) {
        tts = TextToSpeech(appContext, { status ->
            onEngineReady(status, preferredEngine)
        }, preferredEngine)
    }

    private fun onEngineReady(status: Int, attemptedEngine: String?) {
        if (status != TextToSpeech.SUCCESS) {
            if (attemptedEngine == GOOGLE_TTS_ENGINE) {
                Log.w(TAG, "Google TTS engine failed, trying default engine")
                tts?.shutdown()
                tts = null
                createEngine(preferredEngine = null)
                return
            }
            initFinished.set(true)
            Log.w(TAG, "TextToSpeech init failed: $status")
            publishState(TtsState.Unavailable(UnavailableReason.InitFailed))
            return
        }

        initFinished.set(true)
        val engine = tts
        if (engine == null) {
            publishState(TtsState.Unavailable(UnavailableReason.InitFailed))
            return
        }

        englishLocale = resolveEnglishLocale(engine)
        engine.setLanguage(englishLocale)
        Log.d(TAG, "TTS ready locale=$englishLocale")
        publishState(TtsState.Ready)
        flushPending(engine)
    }

    fun speakEnglishWord(word: Word) {
        val text = word.word.trim()
        if (text.isEmpty()) return

        if (_state.value is TtsState.Unavailable) {
            if (!attemptRecovery()) {
                val reason = (_state.value as? TtsState.Unavailable)?.reason
                    ?: UnavailableReason.InitFailed
                emitUiEvent(TtsUiEvent.Unavailable(reason))
                return
            }
        }

        if (isMediaVolumeMuted()) {
            emitUiEvent(TtsUiEvent.VolumeMuted)
            return
        }

        speak(text)
    }

    private fun isMediaVolumeMuted(): Boolean {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return false
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
    }

    private fun speak(text: String) {
        when (val current = _state.value) {
            is TtsState.Ready -> doSpeak(text)
            is TtsState.Initializing -> pendingSpeak = PendingSpeak(text)
            is TtsState.Unavailable -> emitUiEvent(TtsUiEvent.Unavailable(current.reason))
        }
    }

    private fun attemptRecovery(): Boolean {
        val engine = tts ?: return false
        englishLocale = resolveEnglishLocale(engine)
        engine.setLanguage(englishLocale)
        publishState(TtsState.Ready)
        return true
    }

    private fun flushPending(engine: TextToSpeech) {
        val pending = pendingSpeak ?: return
        pendingSpeak = null
        if (isMediaVolumeMuted()) {
            emitUiEvent(TtsUiEvent.VolumeMuted)
            return
        }
        doSpeak(pending.text, engine)
    }

    private fun doSpeak(text: String, engine: TextToSpeech? = tts) {
        val activeEngine = engine ?: run {
            emitUiEvent(TtsUiEvent.SpeakFailed)
            return
        }
        applyEnglishLocale(activeEngine)
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        val result = activeEngine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "minlish_en_${System.currentTimeMillis()}",
        )
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak() ERROR textLen=${text.length} locale=$englishLocale")
            emitUiEvent(TtsUiEvent.SpeakFailed)
        }
    }

    private fun applyEnglishLocale(engine: TextToSpeech) {
        val setResult = engine.setLanguage(englishLocale)
        if (setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            for (candidate in ENGLISH_LOCALE_CANDIDATES) {
                if (engine.setLanguage(candidate) != TextToSpeech.LANG_NOT_SUPPORTED) {
                    englishLocale = candidate
                    return
                }
            }
        }
    }

    /** English only — never [Locale.getDefault] (often vi-VN on your devices). */
    private fun resolveEnglishLocale(engine: TextToSpeech): Locale {
        for (candidate in ENGLISH_LOCALE_CANDIDATES) {
            when (engine.setLanguage(candidate)) {
                TextToSpeech.LANG_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_AVAILABLE,
                TextToSpeech.LANG_MISSING_DATA,
                -> return candidate
            }
        }
        return SPEAK_LOCALE
    }

    private fun publishState(state: TtsState) {
        val apply = {
            _state.value = state
            if (state is TtsState.Unavailable) {
                emitUiEvent(TtsUiEvent.Unavailable(state.reason))
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            apply()
        } else {
            mainHandler.post(apply)
        }
    }

    private fun emitUiEvent(event: TtsUiEvent) {
        _uiEvents.tryEmit(event)
    }

    companion object {
        private const val TAG = "TtsManager"
        private const val INIT_TIMEOUT_MS = 10_000L
        private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"

        val ENGLISH_US: Locale = Locale.forLanguageTag("en-US")
        private val SPEAK_LOCALE: Locale = Locale.US

        private val ENGLISH_LOCALE_CANDIDATES = listOf(
            Locale.US,
            ENGLISH_US,
            Locale.UK,
            Locale.ENGLISH,
        )
    }
}
