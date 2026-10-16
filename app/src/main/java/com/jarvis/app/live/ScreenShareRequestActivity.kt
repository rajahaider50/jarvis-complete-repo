package com.jarvis.app.live

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.app.data.AppLog

/**
 * An invisible screen that shows Android's own "start recording or casting?" question, so the assistant can ask to
 * look at your screen by voice. Android always requires you to press Start yourself.
 */
class ScreenShareRequestActivity : ComponentActivity() {
    private val ask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val d = r.data
        if (r.resultCode == RESULT_OK && d != null) {
            ScreenShareService.frameSink = { CallHost.client?.sendJpeg(it) }
            ScreenShareService.start(applicationContext, r.resultCode, d)
            AppLog.i("ScreenShare", "Screen sharing started")
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            ask.launch(mpm.createScreenCaptureIntent())
        } catch (e: Exception) { AppLog.w(e); finish() }
    }

    companion object {
        fun open(ctx: Context) {
            ctx.startActivity(Intent(ctx, ScreenShareRequestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
}
