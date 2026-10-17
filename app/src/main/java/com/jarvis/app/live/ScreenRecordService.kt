package com.jarvis.app.live

import com.jarvis.app.data.AppLog
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

        CallAudioTap.mic = { buf, len -> mixer.add(buf, len, 16000) }
        CallAudioTap.out = { buf, len -> mixer.add(buf, len, 24000) }
        active = true
        AppLog.i("Rec", "started ${w}x$h -> $sinkUri")

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
        Thread {
            val info = MediaCodec.BufferInfo()
            var eos = false
            while (!eos) {
                val codec = video ?: break
                val idx = try { codec.dequeueOutputBuffer(info, 10_000) } catch (e: Exception) { AppLog.w(e); break }
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { videoFormat = codec.outputFormat; maybeStart() }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && buf != null) {
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
        Thread {
            val frame = ShortArray(1024)
            var pts = 0L
            while (mixer.active) {
                val codec = audio ?: break
                val n = mixer.drain(frame)
                if (n <= 0) continue
                val idx = try { codec.dequeueInputBuffer(10_000) } catch (e: Exception) { AppLog.w(e); break }
                if (idx >= 0) {
                    val ib = codec.getInputBuffer(idx) ?: continue
                    ib.clear()
                    for (i in 0 until n) { val s = frame[i].toInt(); ib.put(s.toByte()); ib.put((s shr 8).toByte()) }
                    codec.queueInputBuffer(idx, 0, n * 2, pts, 0)
                    pts += n.toLong() * 1_000_000L / outRate
                }
            }
        }
        Thread {
            val info = MediaCodec.BufferInfo()
            var eos = false
            while (!eos) {
                val codec = audio ?: break
                val idx = try { codec.dequeueOutputBuffer(info, 10_000) } catch (e: Exception) { AppLog.w(e); break }
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { audioFormat = codec.outputFormat; maybeStart() }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && buf != null) {
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
    }

    override fun onDestroy() { cleanup(); super.onDestroy() }
}
