package com.jarvis.app.live

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jarvis.app.data.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** The small offline speech models (about 40 MB each) used by the keyless wake phrase engine. Downloaded once. */
object VoskModels {
    class Info(val label: String, val folder: String, val url: String)

    val all = mapOf(
        "en" to Info("English", "vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"),
        "hi" to Info("Hindi", "vosk-model-small-hi-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip")
    )

    /** -1 = idle, 0..1 = downloading */
    var progress by mutableStateOf(-1f)
    var error by mutableStateOf<String?>(null)

    fun dir(ctx: Context, lang: String): File = File(File(ctx.filesDir, "vosk"), (all[lang] ?: all.getValue("en")).folder)
    fun ready(ctx: Context, lang: String): Boolean = File(dir(ctx, lang), "am").exists()

    suspend fun download(ctx: Context, lang: String) = withContext(Dispatchers.IO) {
        val info = all[lang] ?: return@withContext
        error = null; progress = 0f
        val root = File(ctx.filesDir, "vosk"); root.mkdirs()
        val tmp = File(root, "download.zip")
        try {
            val c = URL(info.url).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 30000
            val total = c.contentLengthLong.coerceAtLeast(1)
            var done = 0L
            c.inputStream.use { input -> tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n); done += n; progress = (done.toFloat() / total) * 0.9f }
            } }
            ZipInputStream(tmp.inputStream()).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    val target = File(root, e.name)
                    if (!target.canonicalPath.startsWith(root.canonicalPath)) throw SecurityException("Bad zip entry")   // zip-slip guard
                    if (e.isDirectory) target.mkdirs() else { target.parentFile?.mkdirs(); target.outputStream().use { zip.copyTo(it) } }
                    e = zip.nextEntry
                }
            }
            progress = 1f
        } catch (e: Exception) {
            AppLog.e("VoskModels", e); error = e.message ?: "Download failed"; progress = -1f
        } finally { tmp.delete() }
        if (error == null) progress = -1f
    }
}
