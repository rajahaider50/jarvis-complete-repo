package com.jarvis.app.appskills

import android.content.Context
import android.graphics.PointF
import com.jarvis.app.data.AppLog
import com.jarvis.app.live.CursorGeometry
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A recorded UI action that can be replayed through Accessibility. */
data class AppSkillAction(
    val id: String = UUID.randomUUID().toString(),
    var type: String,                 // tap, long_press, swipe, text, wait, back, home
    var label: String = "",
    var x: Float = 0f,                // normalized -100..100, centre 0, up +
    var y: Float = 0f,
    var toX: Float = 0f,              // swipe end
    var toY: Float = 0f,
    var text: String = "",            // for text
    var durationMs: Long = 300,       // swipe/long press/wait
    var delayMs: Long = 350,          // delay before action
    var screenW: Int = 0,             // captured screen size for reference/scaling
    var screenH: Int = 0,
    var boundsLeft: Int = -1,         // captured element bounds (px), -1 = unknown
    var boundsTop: Int = -1,
    var boundsRight: Int = -1,
    var boundsBottom: Int = -1,
    var nodeText: String = "",        // captured element text/desc
    var nodeId: String = "",          // captured view id
    var verifyText: String = ""       // optional text expected after action
)

data class AppSkillProfile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var packageName: String = "",
    var appLabel: String = "",
    var iconUri: String = "",
    var description: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val actions: MutableList<AppSkillAction> = mutableListOf()
)

object AppSkills {
    private var file: File? = null

    fun init(ctx: Context) {
        if (file == null) file = File(ctx.applicationContext.filesDir, "app_skills.json")
    }

    @Synchronized
    fun all(): List<AppSkillProfile> {
        val f = file ?: return emptyList()
        if (!f.isFile) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i -> fromJson(arr.optJSONObject(i) ?: return@mapNotNull null) }
        } catch (e: Exception) { AppLog.w(e); emptyList() }
    }

    @Synchronized
    fun get(id: String): AppSkillProfile? = all().firstOrNull { it.id == id }

    @Synchronized
    fun save(profile: AppSkillProfile) {
        val list = all().toMutableList()
        profile.updatedAt = System.currentTimeMillis()
        val i = list.indexOfFirst { it.id == profile.id }
        if (i >= 0) list[i] = profile else list.add(0, profile)
        writeAll(list)
    }

    @Synchronized
    fun delete(id: String) { writeAll(all().filterNot { it.id == id }) }

    @Synchronized
    fun profilesForPackage(pkg: String?): List<AppSkillProfile> {
        val p = pkg.orEmpty().trim()
        if (p.isEmpty()) return emptyList()
        return all().filter { it.packageName == p }
    }

    private fun writeAll(list: List<AppSkillProfile>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(toJson(it)) }
            file?.writeText(arr.toString(2))
        } catch (e: Exception) { AppLog.w(e) }
    }

    private fun toJson(p: AppSkillProfile): JSONObject = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("packageName", p.packageName)
        put("appLabel", p.appLabel); put("iconUri", p.iconUri); put("description", p.description)
        put("createdAt", p.createdAt); put("updatedAt", p.updatedAt)
        val aa = JSONArray()
        p.actions.forEach { a ->
            aa.put(JSONObject().apply {
                put("id", a.id); put("type", a.type); put("label", a.label)
                put("x", a.x.toDouble()); put("y", a.y.toDouble())
                put("toX", a.toX.toDouble()); put("toY", a.toY.toDouble())
                put("text", a.text); put("durationMs", a.durationMs); put("delayMs", a.delayMs)
                put("screenW", a.screenW); put("screenH", a.screenH)
                put("boundsLeft", a.boundsLeft); put("boundsTop", a.boundsTop)
                put("boundsRight", a.boundsRight); put("boundsBottom", a.boundsBottom)
                put("nodeText", a.nodeText); put("nodeId", a.nodeId); put("verifyText", a.verifyText)
            })
        }
        put("actions", aa)
    }

    private fun fromJson(o: JSONObject): AppSkillProfile {
        val actions = mutableListOf<AppSkillAction>()
        val aa = o.optJSONArray("actions") ?: JSONArray()
        for (i in 0 until aa.length()) {
            val a = aa.optJSONObject(i) ?: continue
            actions.add(AppSkillAction(
                id = a.optString("id", UUID.randomUUID().toString()),
                type = a.optString("type", "tap"),
                label = a.optString("label"),
                x = a.optDouble("x", 0.0).toFloat(),
                y = a.optDouble("y", 0.0).toFloat(),
                toX = a.optDouble("toX", 0.0).toFloat(),
                toY = a.optDouble("toY", 0.0).toFloat(),
                text = a.optString("text"),
                durationMs = a.optLong("durationMs", 300),
                delayMs = a.optLong("delayMs", 350),
                screenW = a.optInt("screenW", 0),
                screenH = a.optInt("screenH", 0),
                boundsLeft = a.optInt("boundsLeft", -1),
                boundsTop = a.optInt("boundsTop", -1),
                boundsRight = a.optInt("boundsRight", -1),
                boundsBottom = a.optInt("boundsBottom", -1),
                nodeText = a.optString("nodeText"),
                nodeId = a.optString("nodeId"),
                verifyText = a.optString("verifyText")
            ))
        }
        return AppSkillProfile(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Untitled"),
            packageName = o.optString("packageName"),
            appLabel = o.optString("appLabel"),
            iconUri = o.optString("iconUri"),
            description = o.optString("description"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            actions = actions
        )
    }

    /** Convert a recorded action point to current screen pixels. */
    fun toCurrentPx(nx: Float, ny: Float): PointF? {
        val sz = CursorGeometry.screenSizeOrNull() ?: return null
        return CursorGeometry.toPx(CursorGeometry.clamp(nx), CursorGeometry.clamp(ny), sz)
    }
}
