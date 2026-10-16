package com.jarvis.app.assistant

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.jarvis.app.data.AppLog

/** Lets the app lock the screen on your command. It can do nothing else (the policy list only contains "force-lock"). */
class JarvisAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) { AppLog.i("DeviceAdmin", "Screen-lock permission granted") }
    override fun onDisabled(context: Context, intent: Intent) { AppLog.i("DeviceAdmin", "Screen-lock permission removed") }
}

object DeviceAdmin {
    private fun component(ctx: Context) = ComponentName(ctx.applicationContext, JarvisAdminReceiver::class.java)
    private fun dpm(ctx: Context) = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isActive(ctx: Context): Boolean = try { dpm(ctx).isAdminActive(component(ctx)) } catch (e: Exception) { AppLog.w(e); false }

    /** The system screen that asks you to switch the permission on. */
    fun activateIntent(ctx: Context): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(ctx))
        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets the assistant lock your screen when you ask. It cannot do anything else.")

    fun lockNow(ctx: Context): Boolean = try { dpm(ctx).lockNow(); true } catch (e: Exception) { AppLog.w(e); false }

    /** Removes the permission (needed before the app can be uninstalled). */
    fun deactivate(ctx: Context) { try { dpm(ctx).removeActiveAdmin(component(ctx)) } catch (e: Exception) { AppLog.w(e) } }
}
