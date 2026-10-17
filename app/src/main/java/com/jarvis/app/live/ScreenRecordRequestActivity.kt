package com.jarvis.app.live

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.app.data.AppLog

/**
 * Invisible screen that shows Android's own "start recording?" question so the assistant can begin a call
 * recording by voice. Android always requires the user to press Start themselves.
 */
class ScreenRecordRequestActivity : ComponentActivity() {
    private val ask = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val d = r.data
        if (r.resultCode == RESULT_OK && d != null) {
            ScreenRecordService.start(applicationContext, r.resultCode, d)
            AppLog.i("ScreenRecord", "Call recording started")
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
            ctx.startActivity(Intent(ctx, ScreenRecordRequestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
}
