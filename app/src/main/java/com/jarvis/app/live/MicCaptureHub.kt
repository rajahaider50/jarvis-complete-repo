package com.jarvis.app.live

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import com.jarvis.app.data.AppLog
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

/**
 * The ONE owner of the microphone. Everything that needs mic PCM (the Gemini sender, the in-app recorder, the level
 * meter) attaches as a listener; nothing else in the app opens an AudioRecord for a live call.
 *
 * It keeps the audio source constant (VOICE_COMMUNICATION while the call is up) and watches for the exact-zero
 * buffer case (another app / the system recorder holding the mic, or the source going silent): after ~1.5 s of
 * continuous digital silence it releases and re-creates the AudioRecord itself, trying the next audio source with
 * backoff, so the call recovers without the user doing anything.
 */
object MicCaptureHub {
    const val RATE = 16000
    private const val READ_BYTES = 3200          // ~100 ms per buffer
    private const val SILENT_BUFFERS = 15        // ~1.5 s of exact-zero audio before we call it silent

    /** Audio sources tried in order; the first that yields real audio is kept. */
    private val SOURCES = intArrayOf(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.DEFAULT
    )
    private fun srcName(src: Int) = when (src) {
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
        else -> "src$src"
    }

    private val listeners = CopyOnWriteArrayList<(ByteArray, Int) -> Unit>()

    private var appCtx: Context? = null
    private var rec: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private var sourceIdx = 0

    @Volatile var sourceName = "-"; private set
    @Volatile var level = 0f; private set
    @Volatile var silent = false; private set
    @Volatile var foreignRecorder = "-"; private set

    /** Called when the silent state flips (so the UI can show/hide the warning). */
    @Volatile var onSilent: ((Boolean) -> Unit)? = null

    val isRunning: Boolean get() = running

    fun addListener(l: (ByteArray, Int) -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: (ByteArray, Int) -> Unit) { listeners.remove(l) }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(): Boolean {
        if (running) return true
        sourceIdx = 0
        val r = open(SOURCES[sourceIdx])
        if (r == null) { Diag.log("mic", "MicCaptureHub: AudioRecord could not be created"); return false }
        rec = r; running = true; silent = false
        try { r.startRecording() } catch (e: Exception) { Diag.fail("mic.start", e); closeRec(); running = false; return false }
        Diag.micOpens++; Diag.micSource = sourceName; Diag.micState = "recording"
        Diag.log("mic", "MicCaptureHub START source=$sourceName rate=$RATE listeners=${listeners.size}")
        thread = Thread({ loop() }, "jarvis-mic").also { it.start() }
        return true
    }

    @Synchronized
    fun stop() {
        if (!running && rec == null) return
        running = false
        try { thread?.join(400) } catch (e: Exception) { AppLog.w(e) }
        thread = null
        closeRec()
        silent = false; level = 0f
        Diag.micReleases++; Diag.micState = "idle"; Diag.micLevel = 0f
        Diag.log("mic", "MicCaptureHub STOP")
    }

    @SuppressLint("MissingPermission")
    private fun open(src: Int): AudioRecord? {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val r = try {
            AudioRecord(src, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE))
        } catch (e: Exception) { Diag.fail("mic.open(${srcName(src)})", e); return null }
        if (r.state != AudioRecord.STATE_INITIALIZED) { Diag.log("mic", "open(${srcName(src)}) not initialized"); try { r.release() } catch (e: Exception) { }; return null }
        try { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(r.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e) }
        try { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(r.audioSessionId)?.enabled = true } catch (e: Exception) { AppLog.w(e) }
        sourceName = srcName(src)
        return r
    }

    private fun closeRec() {
        val r = rec ?: return
        rec = null
        try { r.stop() } catch (e: Exception) { AppLog.w(e) }
        try { r.release() } catch (e: Exception) { AppLog.w(e) }
    }

    private fun loop() {
        try { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (e: Exception) { AppLog.w(e) }
        val buf = ByteArray(READ_BYTES)
        var zeroRun = 0
        Diag.log("mic", "capture thread started")
        while (running) {
            val r = rec ?: break
            val n = try { r.read(buf, 0, buf.size) } catch (e: Exception) { Diag.fail("mic.read", e); -1 }
            if (n <= 0) { if (n < 0) { Diag.log("mic", "read error $n"); try { Thread.sleep(20) } catch (e: Exception) { } }; continue }

            // peak level + exact-zero detection
            var allZero = true
            var sum = 0.0
            var k = 0
            while (k + 1 < n) {
                val s = ((buf[k + 1].toInt() shl 8) or (buf[k].toInt() and 0xFF)).toShort().toInt()
                if (s != 0) allZero = false
                sum += s.toDouble() * s; k += 2
            }
            val rms = sqrt(sum / (n / 2).coerceAtLeast(1)) / 32768.0
            val lvl = (rms * 7).toFloat().coerceIn(0f, 1f)
            level = lvl; Diag.micLevel = lvl

            if (allZero) zeroRun++ else zeroRun = 0
            val nowSilent = zeroRun >= SILENT_BUFFERS
            if (nowSilent != silent) {
                silent = nowSilent
                if (nowSilent) { Diag.micZero(); Diag.foreignRecorder = foreignRecorder() } else Diag.log("mic", "audio returned (level=${"%.3f".format(lvl)})")
                onSilent?.invoke(nowSilent)
            }

            // fan-out to every consumer (Gemini sender, in-app recorder, ...)
            for (l in listeners) { try { l(buf, n) } catch (e: Exception) { Diag.fail("mic.listener", e) } }

            if (nowSilent) recover(zeroRun).also { if (it) zeroRun = 0 }
        }
        Diag.log("mic", "capture thread ended")
    }

    /** Release + re-create with the next audio source, with backoff. Returns true if real audio came back. */
    private fun recover(zeroRun: Int): Boolean {
        if (zeroRun % SILENT_BUFFERS != 0) return false          // don't retry on every silent buffer
        val attempt = zeroRun / SILENT_BUFFERS
        if (attempt > 3) return false
        Diag.log("mic", "silent: recovery attempt $attempt/3")
        closeRec()
        for (step in 0 until SOURCES.size) {
            sourceIdx = (sourceIdx + 1) % SOURCES.size
            val r = open(SOURCES[sourceIdx]) ?: continue
            rec = r
            try { r.startRecording() } catch (e: Exception) { Diag.fail("mic.recover.start", e); closeRec(); continue }
            Diag.micOpens++; Diag.micSource = sourceName
            // probe ~600 ms: any non-zero sample means the mic is alive again
            val probe = ByteArray(READ_BYTES)
            val t0 = System.currentTimeMillis()
            var voiced = false
            while (System.currentTimeMillis() - t0 < 600) {
                val m = try { r.read(probe, 0, probe.size) } catch (e: Exception) { -1 }
                if (m > 0 && probe.any { it.toInt() != 0 }) { voiced = true; break }
            }
            if (voiced) {
                Diag.micRecoveries++; Diag.log("mic", "RECOVERED with source=$sourceName")
                silent = false; level = 0f
                onSilent?.invoke(false)
                return true
            }
            Diag.log("mic", "source=$sourceName still silent")
            closeRec()
            try { Thread.sleep(300L * (step + 1)) } catch (e: Exception) { }
        }
        Diag.log("mic", "recovery failed - another app may be holding the microphone")
        // keep a record so we can still read (and the user sees the warning with the holder name)
        val r = open(SOURCES[sourceIdx]) ?: return false
        rec = r
        try { r.startRecording() } catch (e: Exception) { AppLog.w(e) }
        Diag.micSource = sourceName
        return false
    }

    /** Best-effort: which other app is recording right now (may be limited on Android 10+). */
    fun foreignRecorder(): String {
        val ctx = appCtx ?: return "-"
        return try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mine = ctx.packageName
            val others = am.activeRecordingConfigurations.orEmpty().filter { c ->
                val pkg = if (Build.VERSION.SDK_INT >= 29) c.clientPackageName else null
                pkg != null && pkg != mine && pkg != "android"
            }
            if (others.isEmpty()) "-" else others.joinToString(", ") { c ->
                val pkg = if (Build.VERSION.SDK_INT >= 29) c.clientPackageName else "?"
                "$pkg (src=${c.clientAudioSource})"
            }
        } catch (e: Exception) { Diag.fail("mic.foreignRecorder", e); "-" }
    }

    fun setContext(ctx: Context) { if (appCtx == null) appCtx = ctx.applicationContext }
}
