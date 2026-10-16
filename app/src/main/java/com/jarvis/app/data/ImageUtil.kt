package com.jarvis.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtil {
    fun toJpeg(bmp: Bitmap, maxDim: Int, quality: Int, rotate: Int = 0): ByteArray {
        var b = bmp
        if (rotate != 0) b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rotate.toFloat()) }, true)
        val s = maxDim.toFloat() / maxOf(b.width, b.height)
        if (s < 1f) b = Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    fun fromUri(ctx: Context, uri: Uri, maxDim: Int = 1280): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        bmp?.let { toJpeg(it, maxDim, 82) }
    } catch (e: Exception) { AppLog.w(e); null }
}
