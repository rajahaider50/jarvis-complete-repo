package com.jarvis.app.live

import com.jarvis.app.data.AppLog
import com.jarvis.app.data.Prefs
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.nio.ByteBuffer

/**
 * In-app screen recorder: captures the screen (MediaProjection -> H.264) and mixes both sides of the call
 * (user mic + assistant voice, fed via [CallAudioTap]) into an AAC track, writing one .mp4 into Movies/Jarvis.
 * Runs as a foreground service of type mediaProjection.
 *
 * Round 3 (recording): every engine feeds the tap (Gemini Live and the provider pipeline both) and the audio track
 * keeps real time (silence is padded), so neither voice can be missing and the voices stay in sync with the video.
 */
class ScreenRecordService : Service() {
    companion object {
        var active by mutableStateOf(false)
            private set
        var lastFile by mutableStateOf<String?>(null)
            private set

        fun start(ctx: Context, code: Int, data: Intent) {
            val i = Intent(ctx, ScreenRecordService::class.java).putExtra("code", code).putExtra("data", data)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, ScreenRecordService::class.java)) }
    }

    private val outRate = 44100
    private val mixer = AudioMixer(outRate)

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var video: MediaCodec? = null
    private var audio: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var videoTrack = -1
    private var audioTrack = -1
    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var muxerStarted = false
    private val muxLock = Object()

    private var sinkUri: Uri? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.let { @Suppress("DEPRECATION") it.getParcelableExtra<Intent>("data") }
        if (intent == null || data == null) { stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("rec", "Screen recording", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "rec").setContentTitle("Jarvis is recording the call")
            .setContentText("Screen + both voices are being saved").setSmallIcon(android.R.drawable.presence_video_online).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(78, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(78, n)
        return try {
            startRecording(intent.getIntExtra("code", 0), data)
            START_NOT_STICKY
        } catch (e: Exception) { AppLog.w(e); cleanup(); stopSelf(); START_NOT_STICKY }
    }

    private fun startRecording(code: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(code, data)
        projection = p
        val dm = resources.displayMetrics
        var w = minOf(dm.widthPixels, 720); w = w and 1.inv()
        var h = (dm.heightPixels.toFloat() * w / dm.widthPixels).toInt(); h = h and 1.inv()
        val dpi = dm.densityDpi

        setupMuxer()

        val vf = MediaFormat.createVideoFormat("video/avc", w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        video = MediaCodec.createEncoderByType("video/avc").apply {
            configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        val surface = video!!.createInputSurface()
        video!!.start()

        val af = MediaFormat.createAudioFormat("audio/mp4a-latm", outRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        audio = MediaCodec.createEncoderByType("audio/mp4a-latm").apply { configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); start() }

        // Android 14+ requires the callback to be registered before createVirtualDisplay.
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { AppLog.i("Rec", "projection stopped by system"); cleanup(); stopSelf() }
        }, Handler(Looper.getMainLooper()))
        virtualDisplay = p.createVirtualDisplay("jarvis-rec", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null)

        CallAudioTap.resetCounters()
        val tap: (ByteArray, Int, Int) -> Unit = { buf, len, rate -> mixer.add(buf, len, rate) }
        CallAudioTap.mic = tap
        CallAudioTap.out = if (Prefs.recBothVoices) tap else null
        active = true
        Diag.log("rec", "started ${w}x$h -> $sinkUri taps mic=true out=${CallAudioTap.outAttached} rate=44100 bothVoices=${Prefs.recBothVoices}")
        Diag.log("rec", "waiting for the first mic + assistant buffers (a silent side shows up in this dump)")

        drainVideo()
        feedAudio()
    }

    private fun setupMuxer() {
        val name = "Jarvis_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Jarvis")
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) throw IllegalStateException("Could not create the video file")
            sinkUri = uri
            val fd = contentResolver.openFileDescriptor(uri, "w") ?: throw IllegalStateException("Could not open the video file")
            muxer = MediaMuxer(fd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Jarvis")
            if (!dir.exists()) dir.mkdirs()
            val f = File(dir, name)
            lastFile = f.absolutePath
            muxer = MediaMuxer(f.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }
        if (Build.VERSION.SDK_INT >= 29) lastFile = name
    }

    private fun drainVideo() {
        Thread(name = "rec-video") {
            val info = MediaCodec.BufferInfo()
            var eos = false
            var basePts = -1L
            while (!eos) {
                val codec = video ?: break
                val idx = try { codec.dequeueOutputBuffer(info, 10_000) } catch (e: Exception) { Diag.fail("rec.video", e); break }
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { videoFormat = codec.outputFormat; maybeStart() }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && buf != null) {
                            // A surface-fed encoder stamps frames with the DEVICE UPTIME (nanoseconds), so the first video
                            // sample can sit days away from the audio track's zero. Normalise it, otherwise the audio
                            // appears to be "in the past" and players show the video without it.
                            if (info.presentationTimeUs > 0) {
                                if (basePts < 0) { basePts = info.presentationTimeUs; Diag.log("rec", "video first pts=${basePts}us -> normalised to 0") }
                                info.presentationTimeUs -= basePts
                            }
                            synchronized(muxLock) { if (muxerStarted) { buf.position(info.offset); buf.limit(info.offset + info.size); muxer!!.writeSampleData(videoTrack, buf, info) } }
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eos = true
                    }
                }
            }
        }.start()
    }

    private fun feedAudio() {
        Thread(name = "rec-audio-in") {
            val frame = ShortArray(1024)
            var produced = 0L                     // samples already handed to the encoder (the audio timeline)
            var base = System.currentTimeMillis() // wall clock of the very first sample
            var lastLog = 0L
            var warnedMic = false
            var warnedOut = false
            while (mixer.active) {
                val codec = audio ?: break
                // The audio track must keep REAL time: if the mixer has nothing for a moment we pad with silence,
                // otherwise every pause between turns would shift the voices later and later against the video.
                val due = (System.currentTimeMillis() - base) * outRate / 1000L
                if (due - produced > outRate * 3L) base = System.currentTimeMillis() - produced * 1000L / outRate   // resync after a long stall
                var n = if (due > produced) mixer.drain(frame, 60) else 0
                if (n < 0) n = 0
                if (n < frame.size && due > produced + n) {
                    val pad = minOf((due - produced - n).toInt(), frame.size - n)
                    java.util.Arrays.fill(frame, n, n + pad, 0.toShort())
                    n += pad
                }
                if (n <= 0) { Thread.sleep(4); continue }
                val pts = produced * 1_000_000L / outRate
                val idx = try { codec.dequeueInputBuffer(10_000) } catch (e: Exception) { Diag.fail("rec.audio.in", e); break }
                if (idx >= 0) {
                    val ib = codec.getInputBuffer(idx)
                    if (ib == null) { Thread.sleep(4); continue }
                    ib.clear()
                    for (i in 0 until n) { val s = frame[i].toInt(); ib.put(s.toByte()); ib.put((s shr 8).toByte()) }
                    codec.queueInputBuffer(idx, 0, n * 2, pts, 0)
                    produced += n
                    mixer.countOut(n)
                }
                val now = System.currentTimeMillis()
                if (now - lastLog >= 2000) {
                    lastLog = now
                    Diag.recMicBytes = CallAudioTap.micBytes
                    Diag.recOutBytes = CallAudioTap.outBytes
                    Diag.recSamples = mixer.samplesOut
                    Diag.log("rec", "mic=${CallAudioTap.micBytes}B out=${CallAudioTap.outBytes}B mixed=${mixer.samplesOut * 2}B muxStarted=$muxerStarted")
                    if (!warnedMic && CallAudioTap.micBytes == 0L) {
                        warnedMic = true
                        Diag.log("rec", "WARNING: YOUR voice has not reached the recorder (mic=0B) -> the mic tap is not being fed")
                    }
                    if (!warnedOut && CallAudioTap.outBytes == 0L) {
                        warnedOut = true
                        Diag.log("rec", "WARNING: JARVIS's voice has not reached the recorder (out=0B) -> playback is not tapped on this engine")
                    }
                }
            }
        }.start()
        Thread(name = "rec-audio-out") {
            val info = MediaCodec.BufferInfo()
            var eos = false
            var firstAudio = true
            while (!eos) {
                val codec = audio ?: break
                val idx = try { codec.dequeueOutputBuffer(info, 10_000) } catch (e: Exception) { Diag.fail("rec.audio.out", e); break }
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { audioFormat = codec.outputFormat; Diag.log("rec", "audio format ${codec.outputFormat}"); maybeStart() }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && buf != null) {
                            if (firstAudio) { firstAudio = false; Diag.log("rec", "audio first pts=${info.presentationTimeUs}us size=${info.size}B muxStarted=$muxerStarted") }
                            synchronized(muxLock) { if (muxerStarted) { buf.position(info.offset); buf.limit(info.offset + info.size); muxer!!.writeSampleData(audioTrack, buf, info) } }
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eos = true
                    }
                }
            }
        }.start()
    }

    private fun maybeStart() {
        val vf = videoFormat ?: return
        val af = audioFormat ?: return
        synchronized(muxLock) {
            if (muxerStarted) return
            videoTrack = muxer!!.addTrack(vf)
            audioTrack = muxer!!.addTrack(af)
            muxer!!.start()
            muxerStarted = true
            AppLog.i("Rec", "muxer started (video=$videoTrack audio=$audioTrack)")
        }
    }

    private fun cleanup() {
        if (!active && muxer == null) return
        active = false
        Diag.log("rec", "stopping: mic=${CallAudioTap.micBytes}B out=${CallAudioTap.outBytes}B mixed=${mixer.samplesOut * 2}B muxStarted=$muxerStarted file=$lastFile")
        CallAudioTap.mic = null; CallAudioTap.out = null
        mixer.stop()
        try { video?.signalEndOfInputStream() } catch (e: Exception) { AppLog.w(e) }
        try { video?.stop() } catch (e: Exception) { AppLog.w(e) }
        try { audio?.stop() } catch (e: Exception) { AppLog.w(e) }
        try { video?.release() } catch (e: Exception) { AppLog.w(e) }
        try { audio?.release() } catch (e: Exception) { AppLog.w(e) }
        video = null; audio = null
        try { virtualDisplay?.release() } catch (e: Exception) { AppLog.w(e) }
        virtualDisplay = null
        try { projection?.stop() } catch (e: Exception) { AppLog.w(e) }
        projection = null
        synchronized(muxLock) {
            try { if (muxerStarted) muxer?.stop() } catch (e: Exception) { AppLog.w(e) }
            try { muxer?.release() } catch (e: Exception) { AppLog.w(e) }
            muxer = null; muxerStarted = false
        }
        AppLog.i("Rec", "saved $lastFile")
        Diag.log("rec", "saved $lastFile")
    }

    override fun onDestroy() { cleanup(); super.onDestroy() }
}
