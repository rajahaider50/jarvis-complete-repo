package com.jarvis.app.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import com.jarvis.app.AssistantActivity

/**
 * Registers the app as a real Android "digital assistant" (the same slot Google Assistant uses).
 * After you choose it in Settings > Apps > Default apps > Digital assistant app, the system opens it
 * from the power-button long-press, the home-button hold or the corner-swipe gesture, even when the app is closed.
 */
class JarvisInteractionService : VoiceInteractionService()

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisSession(this)
}

class JarvisSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        try {
            context.startActivity(Intent(context, AssistantActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } catch (e: Exception) { com.jarvis.app.data.AppLog.w(e) }
        hide()
    }
}

/** Required by Android for assistant apps; the real listening is done by the live call, so this only reports "not used". */
class JarvisRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        try { listener?.error(SpeechRecognizer.ERROR_CLIENT) } catch (e: Exception) { com.jarvis.app.data.AppLog.w(e) }
    }
    override fun onCancel(listener: Callback?) { }
    override fun onStopListening(listener: Callback?) { }
}
