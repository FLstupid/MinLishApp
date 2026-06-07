package com.example.minlish.ui.components

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.minlish.R
import com.example.minlish.logic.TtsUiEvent
import com.example.minlish.logic.UnavailableReason
import kotlinx.coroutines.flow.Flow

@Composable
fun TtsUnavailableSnackbarEffect(
    events: Flow<TtsUiEvent>,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is TtsUiEvent.Unavailable -> {
                    val message = when (event.reason) {
                        UnavailableReason.InitFailed ->
                            context.getString(R.string.tts_unavailable_init)
                        UnavailableReason.MissingLanguageData ->
                            context.getString(R.string.tts_unavailable_missing_data)
                    }
                    val actionLabel = if (event.reason == UnavailableReason.MissingLanguageData) {
                        context.getString(R.string.tts_install_action)
                    } else {
                        null
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = actionLabel,
                    )
                    if (result == SnackbarResult.ActionPerformed &&
                        event.reason == UnavailableReason.MissingLanguageData
                    ) {
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    }
                }
                TtsUiEvent.SpeakFailed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.tts_speak_failed),
                        actionLabel = context.getString(R.string.tts_install_action),
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    }
                }
                TtsUiEvent.VolumeMuted -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.tts_volume_muted),
                    )
                }
            }
        }
    }
}
