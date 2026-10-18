package com.jarvis.app.live

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Evidence logger for the bugfix work. Everything goes to logcat with the tag [TAG] (timestamp + thread name)
 * AND into an in-memory ring the Diagnostics screen shows. Counters are kept so a soak test can be read at a glance.
 *
 * This is NOT the error console (that is AppLog); this is the "why is it behaving like this" trace:
 * mic open/close, mic zero-runs and recoveries, playback underruns, server interrupts, reconnects,
 * tool calls, frames sent/dropped and cursor window adds/failures.
 */
object Diag {
    const val TAG = "JARVIS_DIAG"
    private const val MAX = 400

    data class Ev(val t: Long, val thread: String, val msg: String)

    private val main = Handler(Looper.getMainLooper())
    private val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /** Newest last. */
    val events = mutableStateListOf<Ev>()

    // ---- live status (Diagnostics screen) ----
    var micState by mutableStateOf("idle")            // idle / recording / recovering
    var micSource by mutableStateOf("-")
    var micLevel by mutableStateOf(0f)                // 0..1 peak
    var micZeroRuns by mutableStateOf(0)              // how many times mic went exactly silent
    var micRecoveries by mutableStateOf(0)
    var micOpens by mutableStateOf(0)
    var micReleases by mutableStateOf(0)
    var wsState by mutableStateOf("idle")             // idle / connecting / live / reconnecting
    var underruns by mutableStateOf(0)
    var interrupted by mutableStateOf(0)
    var reconnects by mutableStateOf(0)
    var toolCalls by mutableStateOf(0)
    var lastTool by mutableStateOf("-")
    var lastClose by mutableStateOf("-")
    var framesSent by mutableStateOf(0)
    var framesDropped by mutableStateOf(0)
    var cursorAdds by mutableStateOf(0)
    var cursorAddFails by mutableStateOf(0)
    var gestures by mutableStateOf(0)                 // every dispatched gesture (click/swipe)
    var gesturesCompleted by mutableStateOf(0)
    var gesturesFailed by mutableStateOf(0)
    var lastGesture by mutableStateOf("-")            // outcome of the most recent gesture
    var foreignRecorder by mutableStateOf("-")        // package name of another app holding the mic, if any
    var tick by mutableStateOf(0)

    /** Log one line: logcat (authoritative, timestamped) + the in-memory ring. */
    fun log(msg: String) {
        val thread = Thread.currentThread().name
        Log.i(TAG, "[$thread] $msg")
        main.post {
            events.add(Ev(System.currentTimeMillis(), thread, msg))
            if (events.size > MAX) events.removeAt(0)
            tick++
        }
    }

    fun log(tag: String, msg: String) = log("$tag: $msg")

    /** Log a failure with its exception class + message (never swallow silently). */
    fun fail(where: String, e: Throwable) = log("FAIL $where -> ${e.javaClass.simpleName}: ${e.message}")

    fun micZero() { micZeroRuns++; log("mic: ZERO buffer (run #$micZeroRuns)") }

    /** Records a gesture outcome (the whole point of bugfix Masla A: never claim a click that did not happen). */
    fun gestureOutcome(outcome: String) {
        gestures++; lastGesture = outcome
        if (outcome == "COMPLETED") gesturesCompleted++ else gesturesFailed++
        log("gesture: outcome=$outcome (#$gestures, ok=$gesturesCompleted bad=$gesturesFailed)")
    }

    /** Full text for clipboard / file. */
    fun dump(): String {
        val sb = StringBuilder()
        sb.append("JARVIS_DIAG dump ").append(stamp.format(System.currentTimeMillis())).append("\n")
        sb.append("mic state=").append(micState).append(" source=").append(micSource).append(" level=").append(String.format(Locale.US, "%.3f", micLevel))
        sb.append(" opens=").append(micOpens).append(" releases=").append(micReleases).append(" zeroRuns=").append(micZeroRuns).append(" recoveries=").append(micRecoveries).append("\n")
        sb.append("ws=").append(wsState).append(" underruns=").append(underruns).append(" interrupted=").append(interrupted).append(" reconnects=").append(reconnects).append(" lastClose=").append(lastClose).append("\n")
        sb.append("toolCalls=").append(toolCalls).append(" lastTool=").append(lastTool).append(" framesSent=").append(framesSent).append(" framesDropped=").append(framesDropped).append("\n")
        sb.append("cursorAdds=").append(cursorAdds).append(" cursorAddFails=").append(cursorAddFails).append(" foreignRecorder=").append(foreignRecorder).append("\n")
        sb.append("gestures=").append(gestures).append(" completed=").append(gesturesCompleted).append(" failed=").append(gesturesFailed).append(" lastGesture=").append(lastGesture).append("\n")
        sb.append("----- events (newest last) -----\n")
        events.forEach { sb.append(stamp.format(it.t)).append(" [").append(it.thread).append("] ").append(it.msg).append("\n") }
        return sb.toString()
    }
}
