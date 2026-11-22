package com.jarvis.app.appskills

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.jarvis.app.data.AppLog
import com.jarvis.app.live.GestureOutcome
import com.jarvis.app.live.JarvisAccessibilityService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object AppSkillPlayer {
    data class Result(val ok: Boolean, val message: String, val completed: Int)

    private val main = Handler(Looper.getMainLooper())

    fun play(ctx: Context, profile: AppSkillProfile, cb: (Result) -> Unit) {
        Thread {
            val stopped = AtomicBoolean(false)
            val app = ctx.applicationContext
            if (!JarvisAccessibilityService.enabled) {
                post { cb(Result(false, "Accessibility service is off", 0)) }; return@Thread
            }
            if (profile.packageName.isNotBlank() && JarvisAccessibilityService.foregroundPackage() != profile.packageName) {
                launchPackage(app, profile.packageName)
                Thread.sleep(900)
            }
            var done = 0
            for ((i, a) in profile.actions.withIndex()) {
                if (stopped.get()) break
                if (a.delayMs > 0) Thread.sleep(a.delayMs.coerceIn(0, 10_000))
                val ok = runAction(a)
                if (!ok) {
                    post { cb(Result(false, "Step ${i + 1} failed: ${a.label.ifBlank { a.type }}", done)) }
                    return@Thread
                }
                if (a.verifyText.isNotBlank()) {
                    val seen = waitForText(a.verifyText, 2500)
                    if (!seen) {
                        post { cb(Result(false, "Step ${i + 1} verification failed: ${a.verifyText}", done)) }
                        return@Thread
                    }
                }
                done++
            }
            post { cb(Result(true, "Completed ${done}/${profile.actions.size} steps", done)) }
        }.apply { isDaemon = true; start() }
    }

    private fun runAction(a: AppSkillAction): Boolean {
        return try {
            when (a.type) {
                "tap" -> tap(a)
                "long_press" -> longPress(a)
                "swipe" -> swipe(a)
                "text" -> JarvisAccessibilityService.typeText(a.text)
                "wait" -> { Thread.sleep(a.durationMs.coerceIn(50, 30_000)); true }
                "back" -> JarvisAccessibilityService.press("back")
                "home" -> JarvisAccessibilityService.press("home")
                else -> false
            }
        } catch (e: Exception) { AppLog.w(e); false }
    }

    private fun tap(a: AppSkillAction): Boolean {
        // Prefer remembered element when available; fallback to coordinates.
        val target = a.nodeText.ifBlank { a.nodeId.substringAfterLast('/') }
        if (target.isNotBlank()) {
            val hits = JarvisAccessibilityService.findElements(target, 1)
            if (hits.isNotEmpty()) {
                val r = JarvisAccessibilityService.clickElement(target)
                if (!r.contains("not found", true) && !r.contains("did not complete", true)) return true
            }
        }
        val p = AppSkills.toCurrentPx(a.x, a.y) ?: return false
        return JarvisAccessibilityService.tapPxAwait(p.x, p.y) == GestureOutcome.COMPLETED
    }

    private fun longPress(a: AppSkillAction): Boolean {
        val p = AppSkills.toCurrentPx(a.x, a.y) ?: return false
        return JarvisAccessibilityService.longPressPxAwait(p.x, p.y, a.durationMs.coerceIn(300, 3000)) == GestureOutcome.COMPLETED
    }

    private fun swipe(a: AppSkillAction): Boolean {
        val from = AppSkills.toCurrentPx(a.x, a.y) ?: return false
        val to = AppSkills.toCurrentPx(a.toX, a.toY) ?: return false
        val ok = CountDownLatch(1)
        var res = false
        main.post {
            val accepted = JarvisAccessibilityService.swipeNorm(a.x, a.y, a.toX, a.toY, a.durationMs.coerceIn(80, 3000)) { r -> res = r; ok.countDown() }
            if (!accepted) { res = false; ok.countDown() }
        }
        ok.await(3, TimeUnit.SECONDS)
        return res || from != to
    }

    private fun waitForText(text: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val screen = JarvisAccessibilityService.readScreen(2500)
            if (screen.contains(text, ignoreCase = true)) return true
            Thread.sleep(200)
        }
        return false
    }

    private fun launchPackage(ctx: Context, pkg: String) {
        try {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) { AppLog.w(e) }
    }

    private fun post(block: () -> Unit) = main.post(block)
}
