package com.jarvis.app.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jarvis.app.data.AppLog

/**
 * Kept for compatibility. Screen sharing no longer needs Android's MediaProjection question, because it now
 * uses the accessibility service's screenshot (no second projection session). Opening this simply turns sharing on.
 */
class ScreenShareRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { ScreenShareService.start(applicationContext) } catch (e: Exception) { AppLog.w(e) }
        finish()
    }

    companion object {
        fun open(ctx: Context) {
            ctx.startActivity(Intent(ctx, ScreenShareRequestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }
}
